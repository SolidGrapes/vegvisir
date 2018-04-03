package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.model.SignedBlock
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
import edu.cornell.em577.tamperprooflogging.protocol.exception.UnexpectedTerminationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ArrayChannel
import java.util.*
import kotlin.collections.ArrayList

/** Protocol for merging a remote blockchain into the local blockchain */
class MergeRemoteBlockChainProtocol(
    private val applicationContext: Context,
    private val applicationResources: Resources,
    private val localUserId: String,
    private val localTimestamp: Long
) {

    val responseChannel = ArrayChannel<ProtocolMessageProto.ProtocolMessage?>(1)

    /**
     * Retrieves and return the collection of all remote blocks to add, indexed on their cryptographic hashes, as well
     * as the cryptographic hashes of the new frontier set.
     */
    private suspend fun getRemoteBlocksToAdd(
        currentRootBlock: SignedBlock,
        remoteRootBlock: SignedBlock
    ): Pair<HashMap<String, SignedBlock>, List<String>> {
        val blockRepository = BlockChainRepository.getInstance(Pair(applicationContext, applicationResources))
        val stack = ArrayDeque<SignedBlock>(listOf(remoteRootBlock))
        val blocksToAddByCryptoHash = HashMap<String, SignedBlock>()

        var seenCurrentRoot = false
        val frontierHashes = ArrayList<String>()

        if (!blockRepository.containsBlock(remoteRootBlock.cryptoHash)) {
            frontierHashes.add(remoteRootBlock.cryptoHash)
            blocksToAddByCryptoHash[remoteRootBlock.cryptoHash] = remoteRootBlock

            while (stack.isNotEmpty()) {
                val current = stack.pop()
                val blocksToFetch = ArrayList<String>()

                for (parentHash in current.unsignedBlock.parentHashes) {
                    if (parentHash == currentRootBlock.cryptoHash) {
                        seenCurrentRoot = true
                    }
                    if (!blockRepository.containsBlock(parentHash)) {
                        if (parentHash !in blocksToAddByCryptoHash) {
                            blocksToAddByCryptoHash[parentHash] = current
                            blocksToFetch.add(parentHash)
                        }
                    }
                }
                getRemoteBlocks(blocksToFetch).forEach({ stack.push(it) })
            }
        }
        if (!seenCurrentRoot) {
            frontierHashes.add(currentRootBlock.cryptoHash)
        }
        return Pair(blocksToAddByCryptoHash, frontierHashes)
    }

    /**
     * Populates the provided collection of blocks to add with the local and remote sign off blocks. Returns the
     * new root node of the blockchain.
     */
    private suspend fun addSignOffBlocks(
        currentRootBlock: SignedBlock,
        remoteRootBlock: SignedBlock,
        blocksToAddByCryptoHash: HashMap<String, SignedBlock>,
        frontierHashes: List<String>
    ): SignedBlock {
        val (remoteUserId, remoteTimestamp) = getRemoteSignOffData()
        val remoteSignOffBlockParentHashes = if (remoteTimestamp > localTimestamp)
            listOf(currentRootBlock.cryptoHash)
        else
            frontierHashes
        val localSignOffBlockParentHashes = if (localTimestamp > remoteTimestamp)
            listOf(remoteRootBlock.cryptoHash)
        else
            frontierHashes
        val remoteSignOffBlock = SignedBlock.generateSignOff(
            remoteUserId,
            remoteTimestamp,
            remoteSignOffBlockParentHashes,
            applicationResources
        )
        val localSignOffBlock = SignedBlock.generateSignOff(
            localUserId,
            localTimestamp,
            localSignOffBlockParentHashes,
            applicationResources
        )
        blocksToAddByCryptoHash[remoteSignOffBlock.cryptoHash] = remoteSignOffBlock
        blocksToAddByCryptoHash[localSignOffBlock.cryptoHash] = localSignOffBlock
        return if (remoteSignOffBlock.cryptoHash in localSignOffBlock.unsignedBlock.parentHashes) {
                localSignOffBlock
            } else {
                remoteSignOffBlock
            }
    }

    fun execute(): Deferred<Unit> {
        return async(CommonPool) {
            val blockRepository = BlockChainRepository.getInstance(Pair(applicationContext, applicationResources))
            val currentRootBlock = blockRepository.getRootBlock()
            try {
                val remoteRootBlock = getRemoteRootBlock()
                if (remoteRootBlock.cryptoHash == currentRootBlock.cryptoHash) {
                    return@async
                }
                val (blocksToAddByCryptoHash, frontierHashes) = getRemoteBlocksToAdd(currentRootBlock, remoteRootBlock)
                val root = addSignOffBlocks(currentRootBlock, remoteRootBlock, blocksToAddByCryptoHash, frontierHashes)
                blockRepository.updateBlockChain(blocksToAddByCryptoHash.values.toList(), root)
                val mergeCompleteMessage = ProtocolMessageProto.ProtocolMessage.newBuilder()
                    .setType(ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_COMPLETE)
                    .setNoBody(true)
                    .build()
                    .toByteArray()
                // Send mergeCompleteMessage on outgoing connection.
            }
            catch (ute: UnexpectedTerminationException) {}
        }
    }

    private suspend fun getRemoteRootBlock(): SignedBlock {
        val request = ProtocolMessageProto.ProtocolMessage.newBuilder()
            .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_REQUEST)
            .setNoBody(true)
            .build()
            .toByteArray()
        // Send request on outgoing connection.

        while (true) {
            val response = responseChannel.receive()
            if (response == null) {
                throw UnexpectedTerminationException("Network exception detected")
            } else if (response.type ==
                ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_RESPONSE) {
                if (response.getRemoteRootBlockResponse.failedToRetrieve) {
                    throw UnexpectedTerminationException("Remote exception detected")
                }
                return SignedBlock.fromProto(response.getRemoteRootBlockResponse.remoteRootBlock)
            }
        }
    }

    private suspend fun getRemoteBlocks(cryptoHashes: List<String>): List<SignedBlock> {
        val request = ProtocolMessageProto.ProtocolMessage.newBuilder()
            .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_REQUEST)
            .setGetRemoteBlocksRequest(
                ProtocolMessageProto.GetRemoteBlocksRequest.newBuilder()
                    .addAllCryptoHashes(cryptoHashes)
                    .build()
            ).build()
            .toByteArray()
        // Send request on outgoing connection.

        while (true) {
            val response = responseChannel.receive()
            if (response == null) {
                throw UnexpectedTerminationException("Network exception detected")
            } else if (response.type ==
                ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_RESPONSE) {
                if (response.getRemoteBlocksResponse.failedToRetrieve) {
                    throw UnexpectedTerminationException("Remote exception detected")
                }
                return response.getRemoteBlocksResponse.remoteBlocksList.map { SignedBlock.fromProto(it) }
            }
        }
    }

    private suspend fun getRemoteSignOffData(): Pair<String, Long> {
        val request = ProtocolMessageProto.ProtocolMessage.newBuilder()
            .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_SIGN_OFF_DATA_REQUEST)
            .setNoBody(true)
            .build()
            .toByteArray()
        // Send request on outgoing connection.

        while (true) {
            val response = responseChannel.receive()
            if (response == null) {
                throw UnexpectedTerminationException("Network exception detected")
            } else if (response.type ==
                ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_SIGN_OFF_DATA_RESPONSE) {
                if (response.getRemoteSignOffDataResponse.failedToRetrieve) {
                    throw UnexpectedTerminationException("Remote exception detected")
                }
                return Pair(response.getRemoteSignOffDataResponse.remoteUserId,
                    response.getRemoteSignOffDataResponse.remoteTimestamp)
            }
        }
    }
}