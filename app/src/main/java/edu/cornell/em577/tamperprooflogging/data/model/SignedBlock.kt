package edu.cornell.em577.tamperprooflogging.data.model

import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.util.hexStringToByteArray
import edu.cornell.em577.tamperprooflogging.util.toHex
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature

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
        digest.digest().toHex()
    }

    companion object {
        private const val SIGNATURE = "signature"

        /** Creates a signed block from a ProtocolMessageProto SignedBlock. */
        fun fromProto(protoSignedBlock: ProtocolMessageProto.SignedBlock): SignedBlock {
            return SignedBlock(
                UnsignedBlock.fromProto(protoSignedBlock.unsignedBlock),
                protoSignedBlock.signature
            )
        }

        /** Creates a signed block from a Json-formatted (created with toJson) signed block. */
        fun fromJson(properties: Map<String, Any>): SignedBlock {
            return SignedBlock(UnsignedBlock.fromJson(properties), properties[SIGNATURE] as String)
        }

        /**
         * Generates a self-signed admin certificate block given the admin password and the
         * UserDataRepository singleton object. Assumes the generated block will be used as the
         * genesis block of the blockchain.
         */
        fun generateAdminCertificate(userRepo: UserDataRepository, password: String): SignedBlock {
            val adminCertificate = UnsignedBlock.generateAdminCertificate(userRepo)
            val privateKey = userRepo.loadAdminPrivateKey(password)
            return SignedBlock(adminCertificate, adminCertificate.sign(privateKey))
        }

        /**
         * Generates a admin-signed user certificate, provided the parent block hashes,
         * UserDataRepository singleton object, and the admin password.
         */
        fun generateUserCertificate(
            userRepo: UserDataRepository,
            password: String,
            parentHashes: List<String>
        ): SignedBlock {
            val userCertificate = UnsignedBlock.generateUserCertificate(userRepo, parentHashes)
            val privateKey = userRepo.loadAdminPrivateKey(password)
            return SignedBlock(userCertificate, userCertificate.sign(privateKey))
        }

        /**
         * Generates a user-signed proof of witness block given the parent hash blocks,
         * UserDataRepository singleton object, user password, and the local timestamp.
         */
        fun generateProofOfWitness(
            userRepo: UserDataRepository,
            password: String,
            parentHashes: List<String>,
            localTimestamp: Long
        ): SignedBlock {
            val proofOfWitness = UnsignedBlock.generateProofOfWitness(
                userRepo, parentHashes, localTimestamp)
            val privateKey = userRepo.loadUserPrivateKey(password)
            return SignedBlock(proofOfWitness, proofOfWitness.sign(privateKey))
        }
    }

    /** Serializes this signed block to a ProtocolMessageProto SignedBlock. */
    fun toProto(): ProtocolMessageProto.SignedBlock {
        return ProtocolMessageProto.SignedBlock.newBuilder()
            .setUnsignedBlock(unsignedBlock.toProto())
            .setSignature(signature)
            .build()
    }

    /**
     * Serializes this signed block to a Json format compatible with Couchbase Mobile's document
     * store.
     */
    fun toJson(): HashMap<String, Any> {
        val properties = unsignedBlock.toJson()
        properties[SIGNATURE] = signature
        return properties
    }

    /**
     * Using the provided public key, check whether this signed block was signed by the public
     * key's private key pair.
     */
    fun verify(publicKey: PublicKey): Boolean {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKey)
        sig.update(unsignedBlock.userId.toByteArray())
        sig.update(unsignedBlock.timestamp.toString().toByteArray())
        sig.update(unsignedBlock.location.toByteArray())
        sig.update(unsignedBlock.parentHashes.sorted().toString().toByteArray())
        sig.update(unsignedBlock.transactions.map { it.toString() }.sorted().toString().toByteArray())
        return sig.verify(signature.hexStringToByteArray())
    }
}