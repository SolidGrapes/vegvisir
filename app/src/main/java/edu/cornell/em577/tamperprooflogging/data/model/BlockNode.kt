package edu.cornell.em577.tamperprooflogging.data.model

/** A node in the directed acyclic graph of blocks */
data class BlockNode(val block: Block, val parents: List<BlockNode>) {
    /**
     * Bundled set of transactions together with the requester's metadata and the
     * parent blocks' cryptographic hashes. Includes a cryptographic signature of the block
     * using the requester's private key.
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
}

