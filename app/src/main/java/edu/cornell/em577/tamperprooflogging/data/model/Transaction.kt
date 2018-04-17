package edu.cornell.em577.tamperprooflogging.data.model

import com.vegvisir.data.ProtocolMessageProto

/** Core data in a block */
data class Transaction(
    val type: TransactionType,
    val content: String,
    val comment: String
) {
    enum class TransactionType {
        CERTIFICATE,
        RECORD_ACCESS,
        PROOF_OF_WITNESS,
        REVOKE_CERTIFICATE
    }

    companion object {
        private const val TYPE = "type"
        private const val CONTENT = "content"
        private const val COMMENT = "comment"

        fun fromProto(protoTransaction: ProtocolMessageProto.Transaction): Transaction {
            return Transaction(
                TransactionType.valueOf(protoTransaction.type),
                protoTransaction.content,
                protoTransaction.comment
            )
        }

        fun fromJson(properties: Map<String, Any>): Transaction {
            return Transaction(
                TransactionType.valueOf(properties[TYPE] as String),
                properties[CONTENT] as String,
                properties[COMMENT] as String
            )
        }

        fun generateCertificate(userId: String, hexPublicKey: String): Transaction {
            return Transaction(TransactionType.CERTIFICATE, userId, hexPublicKey)
        }

        fun generateRevocation(userIdToRevoke: String): Transaction {
            return Transaction(
                TransactionType.REVOKE_CERTIFICATE,
                userIdToRevoke,
                "Revoking user membership"
            )
        }

        fun generateProofOfWitness(userId: String): Transaction {
            return Transaction(
                TransactionType.PROOF_OF_WITNESS,
                userId,
                "Signing off on all predecessor transactions"
            )
        }
    }

    fun toProto(): ProtocolMessageProto.Transaction {
        return ProtocolMessageProto.Transaction.newBuilder()
            .setType(type.name)
            .setContent(content)
            .setComment(comment)
            .build()
    }

    fun toJson(): Map<String, Any> {
        val properties = HashMap<String, Any>()
        properties[TYPE] = type.toString()
        properties[CONTENT] = content
        properties[COMMENT] = comment
        return properties
    }
}