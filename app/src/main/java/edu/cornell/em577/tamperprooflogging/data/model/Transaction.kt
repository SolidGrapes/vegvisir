package edu.cornell.em577.tamperprooflogging.data.model

import com.vegvisir.data.ProtocolMessageProto

/** Core data in a block */
data class Transaction(
    val type: TransactionType,
    val content: String,
    val comment: String
) {
    /** Type of transaction. */
    enum class TransactionType {
        CERTIFICATE,
        RECORD_REQUEST,
        PROOF_OF_WITNESS,
        REVOKE_CERTIFICATE
    }

    companion object {
        private const val TYPE = "type"
        private const val CONTENT = "content"
        private const val COMMENT = "comment"

        /** Creates a transaction from a ProtocolMessageProto Transaction. */
        fun fromProto(protoTransaction: ProtocolMessageProto.Transaction): Transaction {
            return Transaction(
                TransactionType.valueOf(protoTransaction.type),
                protoTransaction.content,
                protoTransaction.comment
            )
        }

        /** Creates a transaction from a Json-formatted (created with toJson) transaction. */
        fun fromJson(properties: Map<String, Any>): Transaction {
            return Transaction(
                TransactionType.valueOf(properties[TYPE] as String),
                properties[CONTENT] as String,
                properties[COMMENT] as String
            )
        }

        /**
         * Generates a certificate transaction given the userId and his/her corresponding public
         * key in hex string-format.
         */
        fun generateCertificate(userId: String, hexPublicKey: String): Transaction {
            return Transaction(TransactionType.CERTIFICATE, userId, hexPublicKey)
        }

        /** Generates a certificate revocation transaction given the userId to revoke. */
        fun generateRevocation(userIdToRevoke: String): Transaction {
            return Transaction(
                TransactionType.REVOKE_CERTIFICATE,
                userIdToRevoke,
                "Revoking user membership"
            )
        }

        /** Generates a proof-of-witness transaction given the userId of the witness. */
        fun generateProofOfWitness(userId: String): Transaction {
            return Transaction(
                TransactionType.PROOF_OF_WITNESS,
                userId,
                "Signing off on all predecessor transactions"
            )
        }
    }

    /** Serializes this transaction to a ProtocolMessageProto Transaction */
    fun toProto(): ProtocolMessageProto.Transaction {
        return ProtocolMessageProto.Transaction.newBuilder()
            .setType(type.name)
            .setContent(content)
            .setComment(comment)
            .build()
    }

    /**
     * Serializes this transaction to a Json-format compatible with Couchbase Mobile's document
     * store.
     * */
    fun toJson(): Map<String, Any> {
        val properties = HashMap<String, Any>()
        properties[TYPE] = type.toString()
        properties[CONTENT] = content
        properties[COMMENT] = comment
        return properties
    }
}