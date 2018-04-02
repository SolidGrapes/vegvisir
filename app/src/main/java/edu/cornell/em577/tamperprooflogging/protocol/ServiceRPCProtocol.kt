package edu.cornell.em577.tamperprooflogging.protocol

import android.content.Context
import android.content.res.Resources
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
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

    companion object {
        private const val TERMINATE = "Terminate"
    }

    val requestChannel = ArrayChannel<String>(3)

    fun execute(): Deferred<Unit> {
        return async(CommonPool) {
            val blockRepository = BlockChainRepository.getInstance(Pair(applicationContext, applicationResources))

            // Listen for incoming requests on requestChannel. Upon receiving a request, deserialize it, make the
            // appropriate function call, serialize the response, and send it on the outgoing connection object
            // Terminate if the parsed request is TERMINATE or if sending the response on the outgoing connection
            // object throws an exception
        }
    }
}