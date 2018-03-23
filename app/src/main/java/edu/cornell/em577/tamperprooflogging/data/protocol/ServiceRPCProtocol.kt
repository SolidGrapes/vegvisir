package edu.cornell.em577.tamperprooflogging.data.protocol

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ArrayChannel

/**
 * Protocol for servicing remote procedure calls made by the remote endpoint. There can only be one
 * instance of this protocol running at any given time
 */
object ServiceRPCProtocol {

    val requestChannel = ArrayChannel<String>(3)

    fun execute() {
        // Blocks on receiving request messages from the message dispatcher. Upon receiving a
        // request, deserialize the request, parse the procedure call, serialize the response, and
        // send it to the remote endpoint

        // Upon fulfilling the getRemoteClosingBlock request from the remote endpoint, place the
        // generated local block into the channel named localClosingBlockChannel in
        // MergeRemoteBlockChainProtocol

        async(CommonPool) {

        }

        //Release outgoing custom connection objects if networking module does not already do so
    }
}