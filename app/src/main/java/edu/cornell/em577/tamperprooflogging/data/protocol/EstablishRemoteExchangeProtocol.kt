package edu.cornell.em577.tamperprooflogging.data.protocol

import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async

/**
 * Protocol for establishing a connection with a remote endpoint. There can only be one
 * instance of this protocol running at any given time
 */
object EstablishRemoteExchangeProtocol {
    fun execute() {
        async(CommonPool) {
            // Initialize networking module
            while (true) {
                // Go into suspension mode awaiting a signal from the networking module that there is
                // a new incoming connection or a nearby neighbor that can be connected to

                BlockChainRepository.beginExchange()
                setupProtocol()

                // Become the message dispatcher
                while (true) {
                    // Dispatch message either to ServiceRPCProtocol or MergeRemoteBlockChainProtocol via
                    // shared concurrent buffers
                    break
                }
                BlockChainRepository.endExchange()
                // Release incoming custom connection objects if networking module does not already do so
            }
        }
    }

    // Pass in custom (outgoing) connections
    private fun setupProtocol() {
        // Spin up a co-routine to service RPC calls made by the remote endpoint
        // Pass in custom (outgoing) connection objects
        // Ensure that the custom (outgoing) connection objects are thread-safe!
        ServiceRPCProtocol.execute()

        // Spin up a co-routine to merge the remote blockchain into the local blockchain
        // Pass in custom (outgoing) connection objects
        // Ensure that the custom (outgoing) connection objects are thread-safe!
        MergeRemoteBlockChainProtocol.execute()
    }
}