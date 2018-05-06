package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.network.ByteStream
import edu.cornell.em577.tamperprooflogging.protocol.exception.BadMessageException
import edu.cornell.em577.tamperprooflogging.protocol.exception.UnexpectedTerminationException
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import java.util.*


/**
 * Protocol for establishing a connection with a remote endpoint.
 * There is only one instance of this protocol executing at any given time.
 */
class EstablishRemoteExchangeProtocol private constructor(env: Triple<Context, Resources, String>): Runnable {

    companion object :
        SingletonHolder<EstablishRemoteExchangeProtocol, Triple<Context, Resources, String>>(::EstablishRemoteExchangeProtocol) {
        private const val WAIT_TIME_IN_SECONDS = 120L
        private const val SECOND_IN_MILLI = 1000L
    }

    private val userRepo = UserDataRepository.getInstance(Pair(env.first, env.second))
    private val blockRepo = BlockRepository.getInstance(Pair(env.first, env.second))
    private val userPassword = env.third
    private var isRunning = false
    private val byteStream: ByteStream = ByteStream.getInstance(Pair(env.first, userRepo.loadUserMetaData().first))

    /**
     * Executes the protocol to establish a remote exchange of blocks. Ensures only one instance of
     * this protocol is executing at any given time.
     */
    @Synchronized
    fun execute() {
        if (!isRunning) {
            isRunning = true
            byteStream.create()
            Thread(this).start()
        }
    }

    /**
     * Establishes a connection to a remote device, starts the protocols needed to service and
     * execute block merging, and begins dispatching messages from the remote endpoint to these
     * protocols.
     */
    override fun run() {
        while (true) {
            Log.d("Checking", "Establishing Connection")
            val endpointId = byteStream.establishConnection()
            Log.d("Checking", "Established Connection")
            blockRepo.beginExchange()
            val localTimestamp = Calendar.getInstance().timeInMillis

            val serviceRPCProtocol = ServiceRPCProtocol(
                byteStream,
                endpointId,
                blockRepo,
                userRepo,
                userPassword,
                localTimestamp)
            val serviceResult = Thread(serviceRPCProtocol)
            serviceResult.start()

            val mergeRemoteBlockChainProtocol = MergeRemoteBlockChainProtocol(
                byteStream,
                endpointId,
                blockRepo,
                userRepo,
                userPassword,
                localTimestamp)
            val mergeResult = Thread(mergeRemoteBlockChainProtocol)
            mergeResult.start()

            var remoteCompleted = false
            var localCompleted = false
            while (true) {
                try {
                    val incomingMessage = byteStream.recv()
                    val parsedMessage = ProtocolMessageProto.ProtocolMessage.parseFrom(incomingMessage)
                    when (parsedMessage.type) {
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_REQUEST,
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_REQUEST,
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_PROOF_OF_WITNESS_BLOCK_REQUEST,
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_REQUEST ->
                            serviceRPCProtocol.requestChannel.put(parsedMessage)
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_RESPONSE,
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_RESPONSE,
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_PROOF_OF_WITNESS_BLOCK_RESPONSE,
                        ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_RESPONSE ->
                            mergeRemoteBlockChainProtocol.responseChannel.put(parsedMessage)
                        ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_INTERRUPTED -> {
                            throw UnexpectedTerminationException("Unexpected network error during merging")
                        }
                        ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_COMPLETE -> {
                            val mergeCompleteAckMessage = ProtocolMessageProto.ProtocolMessage.newBuilder()
                                .setType(ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_COMPLETE_ACK)
                                .setNoBody(true)
                                .build()
                                .toByteArray()
                            byteStream.send(endpointId, mergeCompleteAckMessage)
                            serviceRPCProtocol.requestChannel.put(parsedMessage)
                            remoteCompleted = true
                        }
                        ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_COMPLETE_ACK -> {
                            localCompleted = true
                        }
                        else -> throw BadMessageException("Improperly formatted message received")
                    }

                    if (remoteCompleted && localCompleted) {
                        if (!mergeRemoteBlockChainProtocol.completed) {
                            throw UnexpectedTerminationException("Endpoint sent Merge Complete Ack when local merge complete did not complete.")
                        }
                        Log.d("Checking", "Completed Merging on both ends")
                        break
                    }
                } catch (e: Exception) {
                    val mergeInterruptedMessage = ProtocolMessageProto.ProtocolMessage.newBuilder()
                        .setType(ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_INTERRUPTED)
                        .setNoBody(true)
                        .build()
                    mergeRemoteBlockChainProtocol.responseChannel.put(mergeInterruptedMessage)
                    serviceRPCProtocol.requestChannel.put(mergeInterruptedMessage)
                    break
                }
            }

            mergeResult.join()
            serviceResult.join()
            blockRepo.endExchange()
            Thread.sleep(WAIT_TIME_IN_SECONDS * SECOND_IN_MILLI)
        }
    }
}