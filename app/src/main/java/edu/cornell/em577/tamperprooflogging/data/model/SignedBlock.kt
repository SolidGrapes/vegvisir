package edu.cornell.em577.tamperprooflogging.data.model

import android.content.res.Resources
import android.util.Base64
import java.security.MessageDigest

/**
 * Includes an UnsignedBlock together with a Base64 encoded cryptographic signature of the
 * block using the requester's private key under the SHA256withRSA scheme.
 * A SHA256 cryptographic hash of the entire block is kept to speed up verification
 */
data class SignedBlock(
    val unsignedBlock: UnsignedBlock,
    val signature: String
) {
    val cryptoHash: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(unsignedBlock.userId.toByteArray())
        digest.update(unsignedBlock.timestamp.toString().toByteArray())
        digest.update(unsignedBlock.location.toByteArray())
        digest.update(unsignedBlock.parentHashes.sorted().toString().toByteArray())
        digest.update(unsignedBlock.transactions.map {
            it.toString()
        }.sorted().toString().toByteArray())
        digest.update(signature.toByteArray())
        Base64.encodeToString(digest.digest(), Base64.DEFAULT)
    }

    companion object {
        private const val SIGNATURE = "signature"

        fun fromJson(properties: Map<String, Any>): SignedBlock {
            return SignedBlock(UnsignedBlock.fromJson(properties), properties[SIGNATURE] as String)
        }

        fun generateSignOff(userId: String,
                            timestamp: Long,
                            parentHashes: List<String>,
                            resources: Resources): SignedBlock {
            val signOff = UnsignedBlock.generateSignOff(userId, timestamp, parentHashes, resources)
            return SignedBlock(signOff, signOff.sign(resources))
        }
    }

    fun toJson(): HashMap<String, Any> {
        val properties = unsignedBlock.toJson()
        properties[SIGNATURE] = signature
        return properties
    }
}