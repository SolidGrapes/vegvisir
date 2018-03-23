package edu.cornell.em577.tamperprooflogging.data.source

import edu.cornell.em577.tamperprooflogging.data.exception.BlockNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.Block
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

/** Repository interfacing with the storage layer to store/retrieve blocks on the blockchain */
object BlockChainRepository {

    // Key-value document cache of blocks
    private val blockNodeStoreByCryptoHash: HashMap<String, Block> = HashMap()

    private val isUpdating = AtomicBoolean(false)

    private const val ROOT = "root"

    init {
        blockNodeStoreByCryptoHash["GenesisCryptoHashUsingSHA2"] =
                Block(
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

        blockNodeStoreByCryptoHash["LeftChildCryptoHashUsingSha2"] =
                Block(
                    "Edwin",
                    10L,
                    "Ithaca",
                    listOf("GenesisCryptoHashUsingSHA2"),
                    listOf(
                        Transaction(
                            Transaction.TransactionType.CERTIFICATE,
                            "WeitaoMedicalRecordId",
                            "Need emergency access to Edwin medical record"
                        )
                    ),
                    "LeftChildSignatureUsingEdwinPrivateKey",
                    "LeftChildCryptoHashUsingSha2"
                )

        blockNodeStoreByCryptoHash["RightChildCryptoHashUsingSha2"] =
                Block(
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

        blockNodeStoreByCryptoHash["EdwinSigCryptoHashUsingSha2"] =
                Block(
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

        val root = Block(
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
    }

    fun containsBlockWithHash(cryptoHash: String): Boolean {
        synchronized(blockNodeStoreByCryptoHash) {
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

    /**
     * Generates a new frontier block with the specified transactions, but is not added to the
     * repository
     */
    fun generateNewBlock(transactions: List<Transaction>): Block {
        TODO()
    }


    /** Indicate to the repository that a remote data exchange is ongoing */
    fun beginExchange() {
        while (!isUpdating.compareAndSet(false, true)) {
            // Spin-lock on flag
        }
    }

    /** Update the cached blockchain */
    fun updateBlockChain(newBlocks: List<Block>, rootBlock: Block) {
        synchronized(blockNodeStoreByCryptoHash) {
            newBlocks.forEach({ blockNodeStoreByCryptoHash[it.cryptoHash] = it })
            blockNodeStoreByCryptoHash[ROOT] = rootBlock
        }
    }

    /** Indicate to the repository that a remote data exchange has completed */
    fun endExchange() {
        isUpdating.set(false)
    }

    /**
     * Retrieves the frontier block in the blockchain
     * @throws BlockNotFoundException if the frontier block was never initialized
     */
    fun getFrontierBlock(): Block {
        synchronized(blockNodeStoreByCryptoHash) {
            return blockNodeStoreByCryptoHash.getOrElse(ROOT, {
                throw BlockNotFoundException("Root block was not initialized in the blockchain")
            })
        }
    }

    /**
     * Retrieves the blocks in the local blockchain corresponding to the provided cryptohashes in
     * the order they were given
     * @throws BlockNotFoundException if a specified cryptohash does not correspond to any block in
     *                                the local blockchain
     */
    fun getBlocks(blockCryptoHashes: Collection<String>): List<Block> {
        synchronized(blockNodeStoreByCryptoHash) {
            return blockCryptoHashes.map {
                blockNodeStoreByCryptoHash.getOrElse(it, {
                    throw BlockNotFoundException("Block with hash $it was not found in the blockchain")
                })
            }

        }
    }
}