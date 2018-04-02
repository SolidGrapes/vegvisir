package edu.cornell.em577.tamperprooflogging.data.source

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.couchbase.lite.Manager
import com.couchbase.lite.QueryOptions
import com.couchbase.lite.android.AndroidContext
import edu.cornell.em577.tamperprooflogging.data.exception.SignedBlockNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.SignedBlock
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import edu.cornell.em577.tamperprooflogging.data.model.UnsignedBlock
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/** Repository interfacing with the storage layer to store/retrieve blocks on the blockchain */
class BlockChainRepository private constructor(env: Pair<Context, Resources>) {

    companion object :
        SingletonHolder<BlockChainRepository, Pair<Context, Resources>>(::BlockChainRepository) {
        private const val ROOT = "Root"
        private const val GENESIS = "Genesis"
        private const val EDWIN_MA = "Edwin_Ma"
        private const val WEITAO_JIANG = "Weitao_Jiang"
        private const val KOLBEINN_KARLSSON = "Kolbeinn_Karlsson"
        private const val ROBBERT_VAN_RENESSE = "Robbert_Van_Renesse"
        private const val ORIGIN = "Origin"
    }

    // Persistent block store
    private val blockstore = Manager(AndroidContext(env.first), Manager.DEFAULT_OPTIONS)
        .getDatabase("blockstore")

    // Write-through cache of signed blocks indexed by cryptographic hashes
    private val signedBlockByCryptoHash: HashMap<String, SignedBlock> = HashMap()

    private val isUpdating = AtomicBoolean(false)

    private val applicationResources = env.second

    init {
        val rootDocument = blockstore.getDocument(ROOT)
        if (rootDocument.properties == null) {
            val genesisUnsigned = UnsignedBlock(
                GENESIS, 0L, ORIGIN, emptyList(), listOf(
                    Transaction.generateCertificate(
                        EDWIN_MA, applicationResources
                    ), Transaction.generateCertificate(
                        WEITAO_JIANG, applicationResources
                    ), Transaction.generateCertificate(
                        KOLBEINN_KARLSSON, applicationResources
                    ), Transaction.generateCertificate(
                        ROBBERT_VAN_RENESSE, applicationResources
                    )
                )
            )
            val genesisSigned = SignedBlock(genesisUnsigned, genesisUnsigned.sign(applicationResources))
            val genesisDocument = blockstore.getDocument(genesisSigned.cryptoHash)
            genesisDocument.putProperties(genesisSigned.toJson())
            rootDocument.putProperties(genesisSigned.toJson())
        }

        val allDocsQuery = blockstore.createAllDocumentsQuery()
        allDocsQuery.setPrefetch(true)
        val result = allDocsQuery.run()
        while (result.hasNext()) {
            val row = result.next()
            signedBlockByCryptoHash[row.documentId] = SignedBlock.fromJson(row.documentProperties as Map<String, Any>)
        }
    }

    /** Check whether the repository contains a signed block with the given crypto hash */
    fun containsBlock(cryptoHash: String): Boolean {
        synchronized(signedBlockByCryptoHash) {
            return cryptoHash in signedBlockByCryptoHash
        }
    }

    /**
     * Generate a new signed root block with the specified transactions, and add it to the
     * repository. Return true if successful, false otherwise.
     */
    fun addBlock(transactions: List<Transaction>): Boolean {
        if (isUpdating.compareAndSet(false, true)) {
            synchronized(signedBlockByCryptoHash) {
                val currentUser = UserDataRepository.getInstance(applicationResources).getCurrentUser()
                val unsignedBlockToAdd = UnsignedBlock(
                    currentUser.userId,
                    Calendar.getInstance().timeInMillis,
                    currentUser.location,
                    listOf(signedBlockByCryptoHash[ROOT]!!.cryptoHash),
                    transactions
                )
                val signedBlockToAdd =
                    SignedBlock(unsignedBlockToAdd, unsignedBlockToAdd.sign(applicationResources))
                val rootDocument = blockstore.getDocument(ROOT)
                val properties = HashMap(rootDocument.properties)
                properties.putAll(signedBlockToAdd.toJson())
                rootDocument.putProperties(properties)
                signedBlockByCryptoHash[ROOT] = signedBlockToAdd
            }
            isUpdating.set(false)
            return true
        }
        return false
    }

    /** Indicate to the repository that a remote data exchange is ongoing */
    fun beginExchange() {
        while (!isUpdating.compareAndSet(false, true)) {
            // Spin-lock on flag
        }
    }

    /**
     * Updates the repository with the signed blocks to add as well as the new signed root block
     */
    fun updateBlockChain(signedBlocksToAdd: List<SignedBlock>, rootSignedBlock: SignedBlock) {
        synchronized(signedBlockByCryptoHash) {
            signedBlocksToAdd.forEach({
                signedBlockByCryptoHash[it.cryptoHash] = it
                val document = blockstore.getDocument(it.cryptoHash)
                document.putProperties(it.toJson())
            })
            val rootDocument = blockstore.getDocument(ROOT)
            rootDocument.putProperties(rootSignedBlock.toJson())
            signedBlockByCryptoHash[ROOT] = rootSignedBlock
        }
    }

    /** Indicate to the repository that a remote data exchange has completed */
    fun endExchange() {
        isUpdating.set(false)
    }

    /**
     * Retrieves the signed root block from the repository
     * @throws SignedBlockNotFoundException if the root block was never initialized
     */
    fun getRootBlock(): SignedBlock {
        synchronized(signedBlockByCryptoHash) {
            return signedBlockByCryptoHash.getOrElse(ROOT, {
                throw SignedBlockNotFoundException(
                    "Root block was not initialized in the blockchain"
                )
            })
        }
    }

    /**
     * Retrieves the signed blocks corresponding to the provided cryptographic hashes in the
     * repository
     * @throws SignedBlockNotFoundException if a provided cryptographic hash does not correspond to
     *                                      any block in the repository
     */
    fun getBlocks(cryptoHashes: Collection<String>): List<SignedBlock> {
        synchronized(signedBlockByCryptoHash) {
            return cryptoHashes.map {
                signedBlockByCryptoHash.getOrElse(it, {
                    throw SignedBlockNotFoundException(
                        "The repository does not hold a signed block with a cryptographic hash $it"
                    )
                })
            }

        }
    }
}