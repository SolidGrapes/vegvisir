package edu.cornell.em577.tamperprooflogging.data.model

import android.content.res.Resources
import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository

/** Core data in a block */
data class Transaction(
    val type: TransactionType,
    val content: String,
    val comment: String
) {
    enum class TransactionType {
        CERTIFICATE,
        RECORD_ACCESS,
        SIGN_OFF
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

        fun generateCertificate(userId: String, resources: Resources): Transaction {
            val userPublicKey =
                UserDataRepository.getInstance(resources).getUser(userId).userPublicKey
            return Transaction(
                TransactionType.CERTIFICATE,
                "$userId : $userPublicKey",
                "Public key certificate for $userId"
            )
        }

        fun generateSignOff(userId: String): Transaction {
            return Transaction(
                TransactionType.SIGN_OFF,
                "Signing Off",
                "$userId has signed off on all predecessor transactions"
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