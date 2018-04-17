package edu.cornell.em577.tamperprooflogging.protocol

import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.exception.SignedBlockNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.SignedBlock
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.protocol.exception.BadMessageException
import edu.cornell.em577.tamperprooflogging.protocol.exception.UnexpectedTerminationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ArrayChannel

/** Protocol for servicing remote procedure calls made by the remote endpoint */
class ServiceRPCProtocol(
    private val blockRepo: BlockRepository,
    private val userRepo: UserDataRepository,
    private val userPassword: String,
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
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_PROOF_OF_WITNESS_BLOCK_REQUEST ->
                                serviceGetRemoteProofOfWitnessBlock(request.getRemoteProofOfWitnessBlockRequest.parentHashesList)
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_REQUEST ->
                                serviceGetRemoteTimestamp()
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
        try {
            val rootBlock = blockRepo.getRootBlock()
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
        try {
            val blocks = blockRepo.getBlocks(cryptoHashes)
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

    private fun serviceGetRemoteProofOfWitnessBlock(parentHashes: List<String>) {
        val proofOfWitness = SignedBlock.generateProofOfWitness(
            userRepo, userPassword, parentHashes, localTimestamp)
        val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
            .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_PROOF_OF_WITNESS_BLOCK_RESPONSE)
            .setGetRemoteProofOfWitnessBlockResponse(
                ProtocolMessageProto.GetRemoteProofOfWitnessBlockResponse.newBuilder()
                    .setRemoteProofOfWitnessBlock(proofOfWitness.toProto())
                    .build()
            ).build()
            .toByteArray()

        // Send response on outgoing connection.
    }

    private fun serviceGetRemoteTimestamp() {
        val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
            .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_RESPONSE)
            .setGetRemoteTimestampResponse(
                ProtocolMessageProto.GetRemoteTimestampResponse.newBuilder()
                    .setRemoteTimestamp(localTimestamp)
                    .build()
            ).build()
            .toByteArray()

        // Send response on outgoing connection.
    }
}