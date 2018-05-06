package edu.cornell.em577.tamperprooflogging.data.source

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.couchbase.lite.Manager
import com.couchbase.lite.android.AndroidContext
import edu.cornell.em577.tamperprooflogging.data.exception.PermissionNotFoundException
import edu.cornell.em577.tamperprooflogging.data.exception.SignedBlockNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.SignedBlock
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import edu.cornell.em577.tamperprooflogging.data.model.UnsignedBlock
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import java.security.PublicKey
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
    private val recordRepo = RecordRepository.getInstance(Pair(env.first, env.second))

    init {
        val allDocsQuery = blockstore.createAllDocumentsQuery()
        allDocsQuery.setPrefetch(true)
        val result = allDocsQuery.run()
        while (result.hasNext()) {
            val row = result.next()
            signedBlockByCryptoHash[row.documentId] =
                    SignedBlock.fromJson(row.documentProperties as Map<String, Any>)
        }
        if (signedBlockByCryptoHash[ROOT] != null) {
            populateRepos()
        }
        Log.d("Checking", "Size of blockstore is ${signedBlockByCryptoHash.size}")
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
        populateRepos()
    }

    /** Populates the record and user repositories with the data found in the blockchain. */
    private fun populateRepos() {
        val rootBlock = signedBlockByCryptoHash[ROOT]!!
        val stack = ArrayDeque<SignedBlock>(listOf(rootBlock))
        while (stack.isNotEmpty()) {
            val currentBlock = stack.pop()
            populateReposWithTransactions(currentBlock.unsignedBlock.transactions)
            for (parentHash in currentBlock.unsignedBlock.parentHashes) {
                val parentBlock = signedBlockByCryptoHash[parentHash]!!
                stack.push(parentBlock)
            }
        }
    }

    /**
     * Populates the record and user repositories with the data found in the provided
     * transactions.
     */
    private fun populateReposWithTransactions(transactions: List<Transaction>) {
        for (transaction in transactions) {
            when (transaction.type) {
                Transaction.TransactionType.CERTIFICATE -> {
                    val userId = transaction.content
                    val hexPublicKey = transaction.comment
                    val publicKey = userRepo.getPublicKeyFromHexString(hexPublicKey)
                    userRepo.addUserCertificate(userId, publicKey)
                }
                Transaction.TransactionType.REVOKE_CERTIFICATE -> {
                    val userId = transaction.content
                    userRepo.removeUserCertificate(userId)
                }
                Transaction.TransactionType.RECORD_ACCESS -> {
                    recordRepo.addRecordAccess(transaction)
                }
                Transaction.TransactionType.PROOF_OF_WITNESS -> {}
            }
        }
    }

    /**
     * Generate a new signed user root block with the specified transactions if the user is active,
     * and add it to the repository. Return true if successful, false otherwise.
     */
    fun addUserBlock(transactions: List<Transaction>, password: String): Boolean {
        if (isUpdating.compareAndSet(false, true)) {
            val (userId, userLocation) = userRepo.loadUserMetaData()
            if (!userRepo.isActiveUser(userId)) {
                throw PermissionNotFoundException("User certificate has been revoked")
            }
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
            populateReposWithTransactions(transactions)
            isUpdating.set(false)
            return true
        }
        return false
    }

    /**
     * Generate a new signed admin root block with the specified transactions, and add it to the
     * repository. Return true if successful, false otherwise.
     */
    fun addAdminBlock(transactions: List<Transaction>, password: String): Boolean {
        if (isUpdating.compareAndSet(false, true)) {
            val (adminId, adminLocation) = userRepo.loadAdminMetaData()
            val unsignedBlockToAdd = UnsignedBlock(
                adminId,
                Calendar.getInstance().timeInMillis,
                adminLocation,
                listOf(signedBlockByCryptoHash[ROOT]!!.cryptoHash),
                transactions
            )
            val privateKey = userRepo.loadAdminPrivateKey(password)
            val signedBlockToAdd =
                SignedBlock(unsignedBlockToAdd, unsignedBlockToAdd.sign(privateKey))
            addBlock(signedBlockToAdd)
            updateRootBlock(signedBlockToAdd)
            populateReposWithTransactions(transactions)
            isUpdating.set(false)
            return true
        }
        return false
    }

    /** Add the specified block to the persistent and cache blockstores. */
    private fun addBlock(block: SignedBlock) {
        signedBlockByCryptoHash[block.cryptoHash] = block
        val document = blockstore.getDocument(block.cryptoHash)
        document.putProperties(block.toJson())
    }

    /** Update the signed root block in the persistent and cache blockstores. */
    private fun updateRootBlock(signedRootBlock: SignedBlock) {
        signedBlockByCryptoHash[ROOT] = signedRootBlock
        val rootDocument = blockstore.getDocument(ROOT)
        val properties = HashMap(rootDocument.properties)
        properties.putAll(signedRootBlock.toJson())
        rootDocument.putProperties(properties)
    }


    /** Indicate to the repository that a remote data exchange is ongoing. */
    fun beginExchange() {
        while (!isUpdating.compareAndSet(false, true)) {
            // Spin-lock on flag
        }
    }

    /**
     * Verifies the provided blocks to ensure that each block is signed by a user that was issued
     * a certificate by the admin. Also verifies that each provided block did not descend from a
     * certificate revocation block that revoked the certificate of the user that signed that block.
     */
    fun verifyBlocks(blocksToVerifyByCryptoHash: HashMap<String, SignedBlock>): Boolean {
        val blocksToVerify = blocksToVerifyByCryptoHash.values.toList()
        val existingCerts = getExistingUserCerts()
        Log.d("Checking", "Extracting Certificate Blocks")
        val (extractedCerts, extractedCertBlocks) = extractUserCert(blocksToVerify)
        Log.d("Checking", "Verifying Certificate Blocks")
        if (!verifyUserCertBlocks(extractedCertBlocks)) {
            return false
        }
        val userCerts = HashMap<String, PublicKey>()
        existingCerts.forEach({ userCerts[it.key] = it.value })
        extractedCerts.forEach({ userCerts[it.key] = it.value })
        Log.d("Checking", "Verifying Signatures")
        if (!verifySignaturesOfBlocks(blocksToVerify, userCerts)) {
            return false
        }
        return verifyAncestryOfBlocks(blocksToVerifyByCryptoHash)
    }

    /**
     * Verify that each provided block does not descend form a certificate revocation block that
     * revoked the certificate of the user that signed that block.
     */
    private fun verifyAncestryOfBlocks(blocksToVerifyByCryptoHash: HashMap<String, SignedBlock>): Boolean {
        for (block in blocksToVerifyByCryptoHash.values) {
            val userId = block.unsignedBlock.userId
            val stack = ArrayDeque<SignedBlock>(listOf(block))
            while (stack.isNotEmpty()) {
                val current = stack.pop()
                if (current.unsignedBlock.transactions.any({
                        it.type == Transaction.TransactionType.REVOKE_CERTIFICATE && it.content == userId
                    })) {
                    return false
                }
                current.unsignedBlock.parentHashes.forEach({
                    if (it in blocksToVerifyByCryptoHash) {
                        stack.push(blocksToVerifyByCryptoHash[it])
                    } else if (it in signedBlockByCryptoHash) {
                        stack.push(signedBlockByCryptoHash[it])
                    }
                })
            }
        }

        return true
    }

    /**
     * Verify that the provided blocks have valid signatures using the provided set of valid user
     * certificates.
     */
    private fun verifySignaturesOfBlocks(blocksToVerify: List<SignedBlock>, userCerts: HashMap<String, PublicKey>): Boolean {
        for (block in blocksToVerify) {
            val publicKey = userCerts[block.unsignedBlock.userId]
            if (publicKey == null || !block.verify(publicKey)) {
                return false
            }
        }

        return true
    }

    /** Verify that the user certificate blocks provided were signed by the admin. */
    private fun verifyUserCertBlocks(blocks: List<SignedBlock>): Boolean {
        val adminPublicKey = userRepo.getAdminPublicKey()!!
        for (block in blocks) {
            if (!block.verify(adminPublicKey)) {
                return false
            }
        }
        return true
    }

    /** Extracts the user certificates from the provided blocks. */
    private fun extractUserCert(blocks: List<SignedBlock>): Pair<HashMap<String, PublicKey>, List<SignedBlock>> {
        val userCerts = HashMap<String, PublicKey>()
        val userCertBlocks = ArrayList<SignedBlock>()
        for (block in blocks) {
            val isCertBlock = block.unsignedBlock.transactions.any({
                it.type == Transaction.TransactionType.CERTIFICATE
            })
            if (isCertBlock) {
                userCertBlocks.add(block)
                for (transaction in block.unsignedBlock.transactions) {
                    if (transaction.type == Transaction.TransactionType.CERTIFICATE) {
                        val userId = transaction.content
                        val hexPublicKey = transaction.comment
                        val publicKey = userRepo.getPublicKeyFromHexString(hexPublicKey)
                        userCerts[userId] = publicKey
                    }
                }
            }
        }
        return Pair(userCerts, userCertBlocks)
    }

    /** Retrieves the existing user certificates from the UserDataRepository. */
    private fun getExistingUserCerts(): HashMap<String, PublicKey> {
        val userCerts = HashMap<String, PublicKey>()
        for ((userId, publicKey) in userRepo.getAllUserCertificates()) {
            userCerts[userId] = publicKey
        }
        return userCerts
    }


    /**
     * Updates the repository with the signed blocks to add as well as the new signed root block.
     * Signed blocks to add must not already exist in the repository
     */
    fun updateBlockChain(signedBlocksToAdd: List<SignedBlock>, rootSignedBlock: SignedBlock) {
        synchronized(signedBlockByCryptoHash) {
            signedBlocksToAdd.forEach({
                addBlock(it)
                populateReposWithTransactions(it.unsignedBlock.transactions)
            })
            updateRootBlock(rootSignedBlock)
        }
    }

    /** Indicate to the repository that a remote data exchange has completed */
    fun endExchange() {
        isUpdating.set(false)
    }

    /** Check whether the repository contains a signed block with the given crypto hash */
    fun containsBlock(cryptoHash: String): Boolean {
        synchronized(signedBlockByCryptoHash) {
            return cryptoHash in signedBlockByCryptoHash
        }
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