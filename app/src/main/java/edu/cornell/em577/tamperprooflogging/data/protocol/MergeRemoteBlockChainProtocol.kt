package edu.cornell.em577.tamperprooflogging.data.protocol

import edu.cornell.em577.tamperprooflogging.data.model.Block
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ArrayChannel
import java.util.*

/**
 * Protocol for merging a remote blockchain into the local blockchain. There can only be one
 * instance of this protocol running at any given time
 */
object MergeRemoteBlockChainProtocol {

    val frontierBlockChannel = ArrayChannel<String>(1)
    val closingBlockChannel = ArrayChannel<String>(1)
    val parentBlocksChannel = ArrayChannel<String>(1)
    val localClosingBlockChannel = ArrayChannel<Block>(1)

    fun execute() {
        async(CommonPool) {
            val remoteFrontierBlock = getRemoteFrontierBlock()
            val stack = ArrayDeque<Block>(listOf(remoteFrontierBlock))
            val blocksToAddByCryptoHash = HashMap<String, Block>()
            if (!BlockChainRepository.containsBlockWithHash(remoteFrontierBlock.cryptoHash)) {
                blocksToAddByCryptoHash[remoteFrontierBlock.cryptoHash] = remoteFrontierBlock

                while (stack.isNotEmpty()) {
                    val current = stack.pop()
                    val blocksToFetch = ArrayList<String>()

                    for (parentHash in current.parentHashes) {
                        if (!BlockChainRepository.containsBlockWithHash(parentHash)) {
                            if (parentHash !in blocksToAddByCryptoHash) {
                                blocksToAddByCryptoHash[parentHash] = current
                                blocksToFetch.add(parentHash)
                            }
                        }
                    }

                    // Need to handle when remote block cannot be retrieved
                    // i.e. getRemoteBlocks throws a BlockNotFoundException
                    getRemoteBlocks(blocksToFetch).forEach({ stack.push(it) })
                }

                // Ensure that these blocking calls safely and promptly terminates when the connection
                // is terminated abruptly before the merging process is complete to free up system
                // resources
                val remoteSigBlock = getRemoteClosingBlock()
                val localSigBlock = localClosingBlockChannel.receive()

                blocksToAddByCryptoHash[remoteSigBlock.cryptoHash] = remoteSigBlock
                blocksToAddByCryptoHash[localSigBlock.cryptoHash] = localSigBlock
                val root = if (remoteSigBlock.cryptoHash in localSigBlock.parentHashes) {
                    localSigBlock
                } else {
                    remoteSigBlock
                }

                // Need to verify the correctness of signatures and hashes on remote blocks to avoid
                // corruption before updating local blockchain. Watch out for cycles
                BlockChainRepository.updateBlockChain(blocksToAddByCryptoHash.values.toList(), root)
            }

            // Release outgoing custom connection objects if networking module does not already do so
        }
    }

    private fun getRemoteFrontierBlock(): Block {
        // Serializes and issues a request to the remote endpoint for its frontier block. Block
        // execution until a response message is received from the message dispatcher. Upon
        // receiving the response message, deserialize the message and return the resulting block
        TODO()
    }

    private fun getRemoteBlocks(childCryptoHashes: List<String>): List<Block> {
        // Serializes and issues a request to the remote endpoint for the list of blocks specified
        // by their cryptographic hashes, in the same order as the request Block execution until a
        // response message is received from the message dispatcher. Upon receiving the response
        // message, deserialize the message and return the resulting parent blocks in a list
        TODO()
    }

    private fun getRemoteClosingBlock(): Block {
        // Serializes and issues a request to the remote endpoint for its connection closing block.
        // Block execution until a response message is received from the message dispatcher. Upon
        // receiving the response message, deserialize the message and return the resulting block
        TODO()
    }
}