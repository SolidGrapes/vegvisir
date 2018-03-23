package edu.cornell.em577.tamperprooflogging.data.model

/**
 * Bundled set of transactions together with the requester's metadata and the
 * parent blocks' cryptographic hashes. Includes a cryptographic signature of the block
 * using the requester's private key. A cryptographic hash of the entire block is kept to speed up
 * verification
 */
data class Block(
    val userId: String,
    val timestamp: Long,
    val location: String,
    val parentHashes: List<String>,
    val transactions: List<Transaction>,
    val signature: String,
    val cryptoHash: String
)