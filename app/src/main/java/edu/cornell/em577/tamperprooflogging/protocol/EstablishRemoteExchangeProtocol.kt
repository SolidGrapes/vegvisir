package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.protocol.exception.BadMessageException
import edu.cornell.em577.tamperprooflogging.protocol.exception.UnexpectedTerminationException
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import java.util.*

/**
 * Protocol for establishing a connection with a remote endpoint.
 * There is only one instance of this protocol executing at any given time.
 */
class EstablishRemoteExchangeProtocol private constructor(env: Triple<BlockRepository, UserDataRepository, String>) {

    companion object :
        SingletonHolder<EstablishRemoteExchangeProtocol, Triple<BlockRepository, UserDataRepository, String>>(::EstablishRemoteExchangeProtocol)

    private val blockRepo = env.first
    private val userRepo = env.second
    private val userPassword = env.third
    private var isRunning = false

    @Synchronized
    fun execute() {
        if (isRunning) {
            return
        }
        isRunning = true
        async(CommonPool) {
            // Initialize networking module

            while (true) {
                // Listen for connections as well as actively seek out nearby connections periodically

                blockRepo.beginExchange()
                val localTimestamp = Calendar.getInstance().timeInMillis

                // Pass in (outgoing) connection object. Ensure that outgoing connection object is thread-safe!
                // Make sure that connection objects catch network exception and rethrow to
                // UnexpectedTerminationException. Make sure that connection objects send and receive byte arrays.
                val serviceRPCProtocol = ServiceRPCProtocol(
                    blockRepo,
                    userRepo,
                    userPassword,
                    localTimestamp)
                val serviceResult = serviceRPCProtocol.execute()

                // Pass in (outgoing) connection object. Ensure that outgoing connection object is thread-safe!
                // Make sure that connection objects catch network exception and rethrow to
                // UnexpectedTerminationException. Make sure that connection objects send and receive byte arrays.
                val mergeRemoteBlockChainProtocol = MergeRemoteBlockChainProtocol(
                    blockRepo,
                    userRepo,
                    userPassword,
                    localTimestamp)
                val mergeResult = mergeRemoteBlockChainProtocol.execute()

                while (true) {
                    // Listen for message on incoming connection.
                    try {
                        val incomingMessage = "STUB".toByteArray()
                        val parsedMessage = ProtocolMessageProto.ProtocolMessage.parseFrom(incomingMessage)
                        when (parsedMessage.type) {
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_REQUEST,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_REQUEST,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_PROOF_OF_WITNESS_BLOCK_REQUEST ,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_REQUEST->
                                serviceRPCProtocol.requestChannel.send(parsedMessage)
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_RESPONSE,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_RESPONSE,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_PROOF_OF_WITNESS_BLOCK_RESPONSE,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_TIMESTAMP_RESPONSE->
                                mergeRemoteBlockChainProtocol.responseChannel.send(parsedMessage)
                            ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_COMPLETE ->
                                serviceRPCProtocol.requestChannel.send(null)
                            else -> throw BadMessageException("Improperly formatted message received")
                        }
                        if (parsedMessage.type == ProtocolMessageProto.ProtocolMessage.MessageType.MERGE_COMPLETE) {
                            break
                        }
                    } catch (ute: UnexpectedTerminationException) {
                        mergeRemoteBlockChainProtocol.responseChannel.send(null)
                        serviceRPCProtocol.requestChannel.send(null)
                        break
                    }
                }
                mergeResult.await()
                serviceResult.await()
                blockRepo.endExchange()
                // Free (incoming/outgoing) connection object
            }
        }
    }
}