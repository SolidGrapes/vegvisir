package edu.cornell.em577.tamperprooflogging.protocol

import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.exception.SignedBlockNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.SignedBlock
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.network.ByteStream
import java.util.concurrent.LinkedBlockingQueue

/** Protocol for servicing remote procedure calls made by the remote endpoint */
class ServiceRPCProtocol(
    private val byteStream: ByteStream,
    private val endpointId: String,
    private val blockRepo: BlockRepository,
    private val userRepo: UserDataRepository,
    private val userPassword: String,
    private val localTimestamp: Long
) : Runnable {
    val requestChannel = LinkedBlockingQueue<ProtocolMessageProto.ProtocolMessage>()

    /**
     * Listens for messages from the dispatcher, services the requests and sends the response to
     * the remote endpoint.
     */
    override fun run() {
        listener@ while (true) {
            try {
                val request = requestChannel.take()
                when (request.type) {
                    ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_REQUEST ->
                        serviceGetRemoteRootBlock()
                    ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_REQUEST ->
                        serviceGetRemoteBlocks(request.getRemoteBlocksRequest.cryptoHashesList)
                    ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_PROOF_OF_WITNESS_BLOCK_REQUEST ->
                        serviceGetRemoteProofOfWitnessBlock(request.getRemoteProofOfWitnessBlockRequest.parentHashesList)
                    ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_REQUEST ->
                        serviceGetRemoteTimestamp()
                    ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_COMPLETE,
                    ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_INTERRUPTED -> break@listener
                    else -> continue@listener
                }
            } catch (e: Exception) {
                break
            }
        }
    }

    /** Services a request to fetch the local root block. */
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

            
            byteStream.send(endpointId, response)
        } catch (sbnfe: SignedBlockNotFoundException) {
            val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
                .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_RESPONSE)
                .setGetRemoteRootBlockResponse(
                    ProtocolMessageProto.GetRemoteRootBlockResponse.newBuilder()
                        .setFailedToRetrieve(true)
                        .build())
                .build()
                .toByteArray()

            byteStream.send(endpointId, response)
        }
    }

    /** Services a request to fetch the local blocks given the crypto hashes of the blocks. */
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
            
            byteStream.send(endpointId, response)
        } catch (sbnfe: SignedBlockNotFoundException) {
            val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
                .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_RESPONSE)
                .setGetRemoteBlocksResponse(
                    ProtocolMessageProto.GetRemoteBlocksResponse.newBuilder()
                        .setFailedToRetrieve(true)
                        .build()
                ).build()
                .toByteArray()
            
            byteStream.send(endpointId, response)
        }
    }

    /** Services a request to fetch a local proof of witness block given the parent hashes. */
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
        
        byteStream.send(endpointId, response)
    }

    /** Services a request to fetch the local timestamp. */
    private fun serviceGetRemoteTimestamp() {
        val response = ProtocolMessageProto.ProtocolMessage.newBuilder()
            .setType(ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_RESPONSE)
            .setGetRemoteTimestampResponse(
                ProtocolMessageProto.GetRemoteTimestampResponse.newBuilder()
                    .setRemoteTimestamp(localTimestamp)
                    .build()
            ).build()
            .toByteArray()

        byteStream.send(endpointId, response)
    }
}