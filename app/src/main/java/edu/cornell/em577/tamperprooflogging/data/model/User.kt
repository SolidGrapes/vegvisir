package edu.cornell.em577.tamperprooflogging.data.model

import java.security.PrivateKey
import java.security.PublicKey

/** Identity of the principal that the system is executing actions on behalf of */
data class User(
    val userId: String,
    val userPassword: String,
    val location: String,
    val userPublicKey: PublicKey,
    val userPrivateKey: PrivateKey
)