package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.exception.SignedBlockNotFoundException
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
import edu.cornell.em577.tamperprooflogging.protocol.exception.BadMessageException
import edu.cornell.em577.tamperprooflogging.protocol.exception.UnexpectedTerminationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ArrayChannel

/** Protocol for servicing remote procedure calls made by the remote endpoint */
class ServiceRPCProtocol(
    private val applicationContext: Context,
    private val applicationResources: Resources,
    private val localUserId: String,
    private val localTimestamp: Long
) {
    val requestChannel = ArrayChannel<ProtocolMessageProto.ProtocolMessage?>(3)

    fun execute(): Deferred<Unit> {
        return async(CommonPool) {
            listener@ while (true) {
                try {
                    val request = requestChannel.receive()
                    if (request == null) {
                        break
                    } else {
                        when (request.type) {
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_REQUEST ->
                                serviceGetRemoteRootBlock()
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_REQUEST ->
                                serviceGetRemoteBlocks(request.getRemoteBlocksRequest.cryptoHashesList)
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_SIGN_OFF_DATA_REQUEST ->
                                serviceGetRemoteSignOffData()
                            else -> continue@listener
                        }
                    }
                } catch (ute: UnexpectedTerminationException) {
                    break
                } catch (bme: BadMessageException) {
                    break
                }
            }
        }
    }

    private fun serviceGetRemoteRootBlock() {
        val blockRepository = BlockChainRepository.getInstance(Pair(applicationContext, applicationResources))
        try {
            val rootBlock = blockRepository.getRootBlock()
            val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
                .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_RESPONSE)
                .setGetRemoteRootBlockResponse(
                    ProtocolMessageProto.GetRemoteRootBlockResponse.newBuilder()
                        .setRemoteRootBlock(rootBlock.toProto())
                        .build())
                .build()
                .toByteArray()

            // Send response on outgoing connection.
        } catch (sbnfe: SignedBlockNotFoundException) {
            val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
                .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_RESPONSE)
                .setGetRemoteRootBlockResponse(
                    ProtocolMessageProto.GetRemoteRootBlockResponse.newBuilder()
                        .setFailedToRetrieve(true)
                        .build())
                .build()
                .toByteArray()

            // Send response on outgoing connection.
        }
    }

    private fun serviceGetRemoteBlocks(cryptoHashes: List<String>) {
        val blockRepository = BlockChainRepository.getInstance(Pair(applicationContext, applicationResources))
        try {
            val blocks = blockRepository.getBlocks(cryptoHashes)
            val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
                .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_RESPONSE)
                .setGetRemoteBlocksResponse(
                    ProtocolMessageProto.GetRemoteBlocksResponse.newBuilder()
                        .addAllRemoteBlocks(blocks.map { it.toProto() })
                        .build()
                ).build()
                .toByteArray()

            // Send response on outgoing connection.
        } catch (sbnfe: SignedBlockNotFoundException) {
            val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
                .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_RESPONSE)
                .setGetRemoteBlocksResponse(
                    ProtocolMessageProto.GetRemoteBlocksResponse.newBuilder()
                        .setFailedToRetrieve(true)
                        .build()
                ).build()
                .toByteArray()

            // Send response on outgoing connection.
        }
    }

    private fun serviceGetRemoteSignOffData() {
        val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
            .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_SIGN_OFF_DATA_RESPONSE)
            .setGetRemoteSignOffDataResponse(
                ProtocolMessageProto.GetRemoteSignOffDataResponse.newBuilder()
                    .setRemoteUserId(localUserId)
                    .setRemoteTimestamp(localTimestamp)
                    .build()
            ).build()
            .toByteArray()

        // Send response on outgoing connection.
    }
}