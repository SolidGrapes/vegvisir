package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.protocol.exception.BadMessageException
import edu.cornell.em577.tamperprooflogging.protocol.exception.UnexpectedTerminationException
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import java.util.*

/**
 * Protocol for establishing a connection with a remote endpoint. There is only one instance of this protocol
 * executing at any given time.
 */
class EstablishRemoteExchangeProtocol private constructor(env: Pair<Context, Resources>) {

    companion object :
        SingletonHolder<EstablishRemoteExchangeProtocol, Pair<Context, Resources>>(::EstablishRemoteExchangeProtocol)

    private val applicationContext = env.first
    private val applicationResources = env.second

    private var isRunning = false

    @Synchronized
    fun execute() {
        if (isRunning) {
            return
        }
        isRunning = true
        async(CommonPool) {
            val blockRepository = BlockChainRepository.getInstance(Pair(applicationContext, applicationResources))
            // Initialize networking module

            while (true) {
                // Listen for connections as well as actively seek out nearby connections periodically

                val localUserId = UserDataRepository.getInstance(applicationResources)
                    .getCurrentUser().userId
                val localTimestamp = Calendar.getInstance().timeInMillis
                blockRepository.beginExchange()

                // Pass in (outgoing) connection object. Ensure that outgoing connection object is thread-safe!
                // Make sure that connection objects catch network exception and rethrow to
                // UnexpectedTerminationException. Make sure that connection objects send and receive byte arrays.
                val serviceRPCProtocol = ServiceRPCProtocol(
                    applicationContext,
                    applicationResources,
                    localUserId,
                    localTimestamp)
                val serviceResult = serviceRPCProtocol.execute()

                // Pass in (outgoing) connection object. Ensure that outgoing connection object is thread-safe!
                // Make sure that connection objects catch network exception and rethrow to
                // UnexpectedTerminationException. Make sure that connection objects send and receive byte arrays.
                val mergeRemoteBlockChainProtocol = MergeRemoteBlockChainProtocol(
                    applicationContext,
                    applicationResources,
                    localUserId,
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
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_SIGN_OFF_DATA_REQUEST ->
                                serviceRPCProtocol.requestChannel.send(parsedMessage)
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_ROOT_BLOCK_RESPONSE,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_BLOCKS_RESPONSE,
                            ProtocolMessageProto.ProtocolMessage.MessageType.GET_REMOTE_SIGN_OFF_DATA_RESPONSE->
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
                blockRepository.endExchange()
                // Free (incoming/outgoing) connection object
            }
        }
    }
}