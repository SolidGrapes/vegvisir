package edu.cornell.em577.tamperprooflogging.data.source

import android.content.Context
import android.content.res.Resources
import com.couchbase.lite.Manager
import com.couchbase.lite.android.AndroidContext
import edu.cornell.em577.tamperprooflogging.data.exception.SignedBlockNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.SignedBlock
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import edu.cornell.em577.tamperprooflogging.data.model.UnsignedBlock
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/** Repository interfacing with the storage layer to store/retrieve blocks on the blockchain */
class BlockRepository private constructor(env: Pair<Context, Resources>) {

    companion object :
        SingletonHolder<BlockRepository, Pair<Context, Resources>>(::BlockRepository) {
        private const val ROOT = "Root"
    }

    // Persistent block store
    private val blockstore = Manager(AndroidContext(env.first), Manager.DEFAULT_OPTIONS)
        .getDatabase("blockstore")

    // Write-through cache of signed blocks indexed by cryptographic hashes
    private val signedBlockByCryptoHash: HashMap<String, SignedBlock> = HashMap()

    private val isUpdating = AtomicBoolean(false)

    private val userRepo = UserDataRepository.getInstance(Pair(env.first, env.second))

    init {
        val allDocsQuery = blockstore.createAllDocumentsQuery()
        allDocsQuery.setPrefetch(true)
        val result = allDocsQuery.run()
        while (result.hasNext()) {
            val row = result.next()
            signedBlockByCryptoHash[row.documentId] =
                    SignedBlock.fromJson(row.documentProperties as Map<String, Any>)
        }
    }

    /** Bootstrap the block repository */
    fun bootstrap(adminPassword: String) {
        val genesisBlock = SignedBlock.generateAdminCertificate(userRepo, adminPassword)
        val userCertBlock = SignedBlock.generateUserCertificate(
            userRepo,
            adminPassword,
            listOf(genesisBlock.cryptoHash)
        )
        addBlock(genesisBlock)
        addBlock(userCertBlock)
        signedBlockByCryptoHash[ROOT] = userCertBlock
        val rootDocument = blockstore.getDocument(ROOT)
        rootDocument.putProperties(userCertBlock.toJson())
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
    fun addBlock(transactions: List<Transaction>, password: String): Boolean {
        if (isUpdating.compareAndSet(false, true)) {
            synchronized(signedBlockByCryptoHash) {
                val (userId, userLocation) = userRepo.loadUserMetaData()
                val unsignedBlockToAdd = UnsignedBlock(
                    userId,
                    Calendar.getInstance().timeInMillis,
                    userLocation,
                    listOf(signedBlockByCryptoHash[ROOT]!!.cryptoHash),
                    transactions
                )
                val privateKey = userRepo.loadUserPrivateKey(password)
                val signedBlockToAdd =
                    SignedBlock(unsignedBlockToAdd, unsignedBlockToAdd.sign(privateKey))
                addBlock(signedBlockToAdd)
                updateRootBlock(signedBlockToAdd)
            }
            isUpdating.set(false)
            return true
        }
        return false
    }

    private fun addBlock(block: SignedBlock) {
        signedBlockByCryptoHash[block.cryptoHash] = block
        val document = blockstore.getDocument(block.cryptoHash)
        document.putProperties(block.toJson())
    }

    /** Indicate to the repository that a remote data exchange is ongoing */
    fun beginExchange() {
        while (!isUpdating.compareAndSet(false, true)) {
            // Spin-lock on flag
        }
    }

    /**
     * Updates the repository with the signed blocks to add as well as the new signed root block.
     * Signed blocks to add must not already exist in the repository
     */
    fun updateBlockChain(signedBlocksToAdd: List<SignedBlock>, rootSignedBlock: SignedBlock) {
        synchronized(signedBlockByCryptoHash) {
            signedBlocksToAdd.forEach({
                signedBlockByCryptoHash[it.cryptoHash] = it
                val document = blockstore.getDocument(it.cryptoHash)
                document.putProperties(it.toJson())
            })
            updateRootBlock(rootSignedBlock)
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

    /** Update the signed root block in the repository */
    private fun updateRootBlock(signedRootBlock: SignedBlock) {
        signedBlockByCryptoHash[ROOT] = signedRootBlock
        val rootDocument = blockstore.getDocument(ROOT)
        val properties = HashMap(rootDocument.properties)
        properties.putAll(signedRootBlock.toJson())
        rootDocument.putProperties(properties)
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