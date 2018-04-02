package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
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
        SingletonHolder<EstablishRemoteExchangeProtocol, Pair<Context, Resources>>(::EstablishRemoteExchangeProtocol) {
        private const val MERGE_COMPLETE = "Merge Completed"
        private const val TERMINATE = "Terminate"
    }

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

                val localUserId = UserDataRepository.getInstance(applicationResources).getCurrentUser().userId
                val localTimestamp = Calendar.getInstance().timeInMillis
                blockRepository.beginExchange()

                // Ensure that outgoing connection object is thread-safe!
                // Pass in outgoing connection object
                val serviceRPCProtocol =
                    ServiceRPCProtocol(applicationContext, applicationResources, localUserId, localTimestamp)
                val serviceResult = serviceRPCProtocol.execute()

                // Ensure that outgoing connection object is thread-safe!
                // Pass in outgoing connection object
                val mergeRemoteBlockChainProtocol =
                    MergeRemoteBlockChainProtocol(applicationContext, applicationResources, localUserId, localTimestamp)
                val mergeResult = mergeRemoteBlockChainProtocol.execute()

                while (true) {
                    // Dispatch message either to ServiceRPCProtocol or MergeRemoteBlockChainProtocol via
                    // coroutine channels based on message headers.
                    // If a network or remote exception is detected, push a TERMINATE string into all coroutine
                    // channels and exit this inner loop. If a MERGE_COMPLETE is received push a TERMINATE string into
                    // all coroutine channels in ServiceRPCProtocol and exit this inner loop.
                    break
                }
                mergeResult.await()
                serviceResult.await()
                blockRepository.endExchange()
                // Free incoming and outgoing connection object
            }
        }
    }
}