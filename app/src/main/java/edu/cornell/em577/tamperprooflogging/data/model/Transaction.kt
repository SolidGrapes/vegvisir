package edu.cornell.em577.tamperprooflogging.data.model

/** Core data in a block */
data class Transaction(
    val type: TransactionType,
    val subjectBody: String,
    val subjectClosing: String
) {
    enum class TransactionType {
        CERTIFICATE,
        RECORD,
        SIGNATURE
    }

    companion object {
        fun fromString(stringRepr: String): Transaction {
            TODO()
        }
    }
}