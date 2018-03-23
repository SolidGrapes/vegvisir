package edu.cornell.em577.tamperprooflogging.data.source

import edu.cornell.em577.tamperprooflogging.data.model.BlockNode
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

/** Repository interfacing with the storage layer to store/retrieve blocks on the blockchain */
object BlockChainRepository {

    private class BlockNodeMutableWrapper(var BlockNode: BlockNode)

    // Temporary java in-memory underlying key-value document store of blocks until better persistence solution is implemented
    private val blockNodeStoreByCryptoHash: HashMap<String, BlockNode.Block> = HashMap()

    // Temporary java in-memory cache of blocks until better caching solution is implemented
    private val frontierBlockNodeWrapper: BlockNodeMutableWrapper

    private val isUpdating = AtomicBoolean(false)

    private const val ROOT = "root"

    /**
     * Assumes that the blockchain in the underlying data-store is consistent and forms an
     * acyclic directed graph
     */
    private fun formBlockChain(): BlockNode {
        val visitedBlockNodeByCryptoHash = HashMap<String, BlockNode>()
        val stack = ArrayDeque<BlockNode.Block>(listOf(blockNodeStoreByCryptoHash[ROOT]))
        while (stack.isNotEmpty()) {
            val root = stack.pop()
            val blocksToVisit = ArrayList<BlockNode.Block>()
            for (parentHash in root.parentHashes) {
                if (parentHash !in visitedBlockNodeByCryptoHash) {
                    blocksToVisit.add(blockNodeStoreByCryptoHash[parentHash]!!)
                }
            }
            if (blocksToVisit.isEmpty()) {
                val rootNode = BlockNode(
                    root,
                    root.parentHashes.map { visitedBlockNodeByCryptoHash[it]!! })
                visitedBlockNodeByCryptoHash[root.cryptoHash] = rootNode

                if (stack.isEmpty()) {
                    return rootNode
                }
            } else {
                stack.push(root)
                stack.addAll(blocksToVisit)
            }
        }
        throw RuntimeException("Cycle in blockchain found!")
    }

    init {
        blockNodeStoreByCryptoHash["GenesisCryptoHashUsingSHA2"] = BlockNode.Block(
            "Genesis",
            0L,
            "Origin",
            emptyList(),
            listOf(
                Transaction(
                    Transaction.TransactionType.CERTIFICATE,
                    "Edwin : 12345",
                    "EdwinSignedCertificate"
                )
            ),
            "GenesisSignatureUsingAdminPrivateKey",
            "GenesisCryptoHashUsingSHA2"
        )

        blockNodeStoreByCryptoHash["LeftChildCryptoHashUsingSha2"] = BlockNode.Block(
            "Genesis",
            0L,
            "Origin",
            emptyList(),
            listOf(
                Transaction(
                    Transaction.TransactionType.CERTIFICATE,
                    "Edwin : 12345",
                    "EdwinSignedCertificate"
                )
            ),
            "GenesisSignatureUsingAdminPrivateKey",
            "GenesisCryptoHashUsingSHA2"
        )

        blockNodeStoreByCryptoHash["RightChildCryptoHashUsingSha2"] = BlockNode.Block(
            "Weitao",
            20L,
            "New York City",
            listOf("GenesisCryptoHashUsingSHA2"),
            listOf(
                Transaction(
                    Transaction.TransactionType.RECORD,
                    "EdwinMedicalRecordId",
                    "Need emergency access to Edwin medical record"
                )
            ),
            "RightChildSignatureUsingWeitaoPrivateKey",
            "RightChildCryptoHashUsingSha2"
        )

        blockNodeStoreByCryptoHash["EdwinSigCryptoHashUsingSha2"] = BlockNode.Block(
            "Edwin",
            30L,
            "Ithaca",
            listOf("LeftChildCryptoHashUsingSha2", "RightChildCryptoHashUsingSha2"),
            listOf(
                Transaction(
                    Transaction.TransactionType.SIGNATURE,
                    "My predecessor accesses have been recorded onto my blockchain",
                    "Signing off"
                )
            ),
            "EdwinSigSignatureUsingEdwinPrivateKey",
            "EdwinSigCryptoHashUsingSha2"
        )

        val root = BlockNode.Block(
            "Weitao",
            40L,
            "New York City",
            listOf("EdwinSigCryptoHashUsingSha2"),
            listOf(
                Transaction(
                    Transaction.TransactionType.SIGNATURE,
                    "My predecessor accesses have been recorded onto my blockchain",
                    "Signing off"
                )
            ),
            "WeitaoSigSignatureUsingWeitaoPrivateKey",
            "WeitaoSigCryptoHashUsingSha2"
        )
        blockNodeStoreByCryptoHash["WeitaoSigCryptoHashUsingSha2"] = root
        blockNodeStoreByCryptoHash[ROOT] = root
        frontierBlockNodeWrapper = BlockNodeMutableWrapper(formBlockChain())
    }

    fun inBlockChain(cryptoHash: String): Boolean {
        synchronized(frontierBlockNodeWrapper) {
            return cryptoHash in blockNodeStoreByCryptoHash
        }
    }

    /** Create a new block node with the specified transactions, add it to the repository, and update the cache */
    fun addNewBlockNode(transactions: List<Transaction>): Boolean {
        // TODO: Atomically switch on the isUpdating flag if false and synchronize on the internal frontierBlockNodeWrapper instance
        // TODO: Retrieve userId and location of this authenticated user
        // TODO: Generate current timestamp and retrieve cryptoHash of current frontier Block
        // TODO: Sign this new block using the authenticated user's private key
        // TODO: Cryptographically hash the contents of the newly generated block
        // TODO: Strip leading and trailing whitespaces as well. Maintain case-sensitivity when important. Look into employing other data cleaning mechanisms
        // TODO: Order parent hashes and transactions before signing/hashing in order to ensure correct equality checks
        // TODO: Switch off the isUpdating flag
        TODO()
    }

    /** Create a new block with the specified transactions */
    fun createNewBlock(transactions: List<Transaction>): BlockNode.Block {
        TODO()
    }


    /** Indicate to the repository that a remote data exchange is ongoing */
    fun beginExchange() {
        while (!isUpdating.compareAndSet(false, true)) {
            // Spin-lock on flag
        }
    }

    /** Update the cached blockchain */
    fun updateBlockChain(newBlocks: List<BlockNode.Block>, rootBlock: BlockNode.Block) {
        synchronized(frontierBlockNodeWrapper) {
            newBlocks.forEach({ blockNodeStoreByCryptoHash[it.cryptoHash] = it })
            blockNodeStoreByCryptoHash[ROOT] = rootBlock
            frontierBlockNodeWrapper.BlockNode = formBlockChain()
        }
    }

    /** Indicate to the repository that a remote data exchange has completed */
    fun endExchange() {
        isUpdating.set(false)
    }

    fun getBlockChain(): BlockNode {
        synchronized(frontierBlockNodeWrapper) {
            return frontierBlockNodeWrapper.BlockNode
        }
    }

    /**
     * Retrieves and returns the list of parent blocks for each specified block identified by
     * its cryptohash in the order it was given
     */
    fun getBlocks(blockCryptoHashes: List<String>): List<BlockNode.Block> {
        synchronized(frontierBlockNodeWrapper) {
            return blockCryptoHashes.map {
                blockNodeStoreByCryptoHash.getOrElse(
                    it,
                    { throw RuntimeException("Block with hash $it was not found in the blockchain") })
            }

        }
    }
}