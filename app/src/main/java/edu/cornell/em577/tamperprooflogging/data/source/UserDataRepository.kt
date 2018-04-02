package edu.cornell.em577.tamperprooflogging.data.source

import android.content.res.Resources
import com.couchbase.lite.util.IOUtils
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.exception.UserNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.User
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


/** Repository interfacing with a storage layer to store/retrieve user data */
class UserDataRepository private constructor(resources: Resources) {

    companion object : SingletonHolder<UserDataRepository, Resources>(::UserDataRepository) {
        private const val DEFAULT_USER = "Genesis"
    }

    private val userById = HashMap<String, User>()

    @Volatile
    private var currentUser: User

    init {
        val genesisPrivateKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.genesis_private_key))
        val genesisPrivateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(genesisPrivateKeyBytes))
        val genesisPublicKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.genesis_public_key))
        val genesisPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(genesisPublicKeyBytes))
        userById["Genesis"] =
                User("Genesis",
                    "genesis",
                    "Origin",
                    genesisPublicKey,
                    genesisPrivateKey)

        val edwinPrivateKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.edwin_ma_private_key))
        val edwinPrivateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(edwinPrivateKeyBytes))
        val edwinPublicKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.edwin_ma_public_key))
        val edwinPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(edwinPublicKeyBytes))
        userById["Edwin_Ma"] =
                User("Edwin_Ma",
                    "edwin_ma",
                    "Ithaca",
                    edwinPublicKey,
                    edwinPrivateKey)

        val weitaoPrivateKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.weitao_jiang_private_key))
        val weitaoPrivateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(weitaoPrivateKeyBytes))
        val weitaoPublicKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.weitao_jiang_public_key))
        val weitaoPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(weitaoPublicKeyBytes))
        userById["Weitao_Jiang"] =
                User("Weitao_Jiang",
                    "weitao_jiang",
                    "Ithaca",
                    weitaoPublicKey,
                    weitaoPrivateKey)

        val kolbeinnPrivateKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.kolbeinn_karlsson_private_key))
        val kolbeinnPrivateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(kolbeinnPrivateKeyBytes))
        val kolbeinnPublicKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.kolbeinn_karlsson_public_key))
        val kolbeinnPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(kolbeinnPublicKeyBytes))
        userById["Kolbeinn_Karlsson"] =
                User("Kolbeinn_Karlsson",
                    "kolbeinn_karlsson",
                    "Ithaca",
                    kolbeinnPublicKey,
                    kolbeinnPrivateKey)

        val robbertPrivateKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.robbert_van_renesse_private_key))
        val robbertPrivateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(robbertPrivateKeyBytes))
        val robbertPublicKeyBytes = IOUtils.toByteArray(
            resources.openRawResource(R.raw.robbert_van_renesse_public_key))
        val robbertPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(robbertPublicKeyBytes))
        userById["Robbert_Van_Renesse"] =
                User("Robbert_Van_Renesse",
                    "robbert_van_renesse",
                    "Ithaca",
                    robbertPublicKey,
                    robbertPrivateKey)
        currentUser = userById["Genesis"]!!
    }

    fun containsUserWithPassword(userId: String, password: String): Boolean {
        synchronized(userById) {
            if (userById.contains(userId)) {
                return userById[userId]!!.userPassword == password
            }
            return false
        }
    }

    fun getUser(userId: String): User {
        synchronized(userById) {
            if (userById.contains(userId)) {
                return userById[userId]!!
            }
            throw UserNotFoundException("User with $userId does not exist")
        }
    }

    fun getCurrentUser(): User {
        return currentUser
    }

    fun setCurrentUser(user: User) {
        currentUser = user
    }
}