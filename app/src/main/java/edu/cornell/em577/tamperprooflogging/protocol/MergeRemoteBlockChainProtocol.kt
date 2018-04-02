package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
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

    companion object {
        private const val MERGE_COMPLETE = "Merge Completed"
        private const val TERMINATE = "Terminate"
    }

    val getRemoteRootBlockChannel = ArrayChannel<String>(1)
    val getRemoteBlocksChannel = ArrayChannel<String>(1)
    val getRemoteSignOffDataChannel = ArrayChannel<String>(1)

    /**
     * Retrieves and return the collection of all remote blocks to add, indexed on their cryptographic hashes, as well
     * as the cryptographic hashes of the new frontier set.
     */
    private fun getRemoteBlocksToAdd(
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
    private fun addSignOffBlocks(
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
                // Send a MERGE_COMPLETE on the outgoing connection object
            }
            catch (ute: UnexpectedTerminationException) {}
        }
    }

    private fun getRemoteRootBlock(): SignedBlock {
        // Serializes and issues a request to the remote endpoint for its root block. Blocks
        // execution until a response message is received from the message dispatcher. Upon
        // receiving the response message, deserialize the message and return the resulting root block
        // Throw a UnexpectedTerminationException if the parsed response is TERMINATE or if sending the request on
        // the outgoing connection object throws an exception
        TODO()
    }

    private fun getRemoteBlocks(cryptoHashes: List<String>): List<SignedBlock> {
        // Serializes and issues a request to the remote endpoint for a list of signed blocks specified
        // by the provided list of cryptographic hashes, in that order. Blocks execution until a
        // response message is received from the message dispatcher. Upon receiving the response
        // message, deserialize the message and return the resulting list of signed blocks
        // Throw a UnexpectedTerminationException if the parsed response is TERMINATE or if sending the request on
        // the outgoing connection object throws an exception
        TODO()
    }

    private fun getRemoteSignOffData(): Pair<String, Long> {
        // Serializes and issues a request to the remote endpoint for its sign off data.
        // Block execution until a response message is received on getRemoteSignOffDataChannel. Upon receipt,
        // deserialize the message and return the resulting sign off data comprising of remote userId and sign off
        // timestamp.
        // Throw a UnexpectedTerminationException if the parsed response is TERMINATE or if sending the request on
        // the outgoing connection object throws an exception
        TODO()
    }
}