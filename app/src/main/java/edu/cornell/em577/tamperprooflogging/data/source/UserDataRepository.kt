package edu.cornell.em577.tamperprooflogging.data.source

import android.content.Context
import android.content.res.Resources
import com.couchbase.lite.Manager
import com.couchbase.lite.android.AndroidContext
import com.couchbase.lite.util.IOUtils
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import edu.cornell.em577.tamperprooflogging.util.hexStringToByteArray
import edu.cornell.em577.tamperprooflogging.util.toHex
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/** Repository interfacing with a storage layer to store/retrieve user data */
class UserDataRepository private constructor(env: Pair<Context, Resources>) {

    companion object :
        SingletonHolder<UserDataRepository, Pair<Context, Resources>>(::UserDataRepository) {
        private const val ADMIN_NAME = "Certificate Authority"
        private const val ADMIN_LOCATION = "Origin"

        private const val USER = "User"
        private const val USER_ID = "userId"
        private const val USER_LOCATION = "userLocation"
        private const val USER_HASHED_PASS = "userHashedPassword"
        private const val USER_SALT = "userSalt"
        private const val USER_PUBLIC_KEY = "userPublicKey"
        private const val USER_SYM_KEY_IV = "userSymKeyIv"
        private const val ENC_USER_PRIVATE_KEY = "encUserPrivateKey"
        private const val SIG_KEYGEN_ALGO = "RSA"
        private const val ENC_KEYGEN_ALGO = "PBKDF2withHmacSHA1"
        private const val ENC_ALGO = "AES/CBC/PKCS5Padding"
        private const val BASE_ENC_ALGO = "AES"
        private const val SALT_LEN = 8
        private const val ITERATION_COUNT = 65536
        private const val KEY_LEN = 256
    }

    // Persistent user store
    private val userstore = Manager(AndroidContext(env.first), Manager.DEFAULT_OPTIONS)
        .getDatabase("userstore")

    private val applicationResources = env.second
    private var inRegistration: Boolean

    init {
        val userDocument = userstore.getDocument(USER)
        inRegistration = userDocument.properties == null
    }

    /** Returns whether the application is currently in its bootstrapping phase */
    fun inRegistration(): Boolean {
        return inRegistration
    }

    fun authenticateAdmin(password: String): Boolean {
        val salt = getBytesFromRawRes(R.raw.ca_salt)
        val hashedPass = getBytesFromRawRes(R.raw.ca_hashed_password)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(password.toByteArray())
        digest.update(salt)
        return hashedPass contentEquals digest.digest()
    }

    fun loadAdminMetaData(): Pair<String, String> {
        return Pair(ADMIN_NAME, ADMIN_LOCATION)
    }

    fun loadAdminPublicKey(): PublicKey {
        val x509EncodedKeySpec = X509EncodedKeySpec(getBytesFromRawRes(R.raw.ca_public_key))
        val keyFactory = KeyFactory.getInstance(SIG_KEYGEN_ALGO)
        return keyFactory.generatePublic(x509EncodedKeySpec)
    }

    fun loadAdminHexPublicKey(): String {
        return getBytesFromRawRes(R.raw.ca_public_key).toHex()
    }

    fun loadAdminPrivateKey(password: String): PrivateKey {
        val salt = getBytesFromRawRes(R.raw.ca_salt)
        val secret = getSecretSpec(password, salt)
        val cipher = Cipher.getInstance(ENC_ALGO)
        val iv = getBytesFromRawRes(R.raw.ca_sym_key_iv)
        cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))
        val cipherText = getBytesFromRawRes(R.raw.enc_ca_private_key)
        val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(cipher.doFinal(cipherText))
        val keyFactory = KeyFactory.getInstance(SIG_KEYGEN_ALGO)
        return keyFactory.generatePrivate(pkcS8EncodedKeySpec)
    }

    fun authenticateUser(password: String): Boolean {
        val properties = userstore.getDocument(USER).properties
        val salt = (properties[USER_SALT] as String).hexStringToByteArray()
        val hashedPass = (properties[USER_HASHED_PASS] as String).hexStringToByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(password.toByteArray())
        digest.update(salt)
        return digest.digest() contentEquals hashedPass
    }

    fun loadUserMetaData(): Pair<String, String> {
        val properties = userstore.getDocument(USER).properties
        val userId = properties[USER_ID] as String
        val userLocation = properties[USER_LOCATION] as String
        return Pair(userId, userLocation)
    }

    fun loadUserPublicKey(): PublicKey {
        val hexPublicKey = loadUserHexPublicKey()
        val x509EncodedKeySpec = X509EncodedKeySpec(hexPublicKey.hexStringToByteArray())
        val keyFactory = KeyFactory.getInstance(SIG_KEYGEN_ALGO)
        return keyFactory.generatePublic(x509EncodedKeySpec)
    }

    fun loadUserHexPublicKey(): String {
        val properties = userstore.getDocument(USER).properties
        return properties[USER_PUBLIC_KEY] as String
    }

    fun loadUserPrivateKey(password: String): PrivateKey {
        val properties = userstore.getDocument(USER).properties
        val salt = (properties[USER_SALT] as String).hexStringToByteArray()
        val secret = getSecretSpec(password, salt)
        val cipher = Cipher.getInstance(ENC_ALGO)
        val iv = (properties[USER_SYM_KEY_IV] as String).hexStringToByteArray()
        cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))
        val cipherText = (properties[ENC_USER_PRIVATE_KEY] as String).hexStringToByteArray()
        val pkcs8EncodedKeySpec = PKCS8EncodedKeySpec(cipher.doFinal(cipherText))
        val keyFactory = KeyFactory.getInstance(SIG_KEYGEN_ALGO)
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec)
    }

    /**
     * Registers a new user during the application bootstrapping phase.
     * Assumed to be called only once throughout the application's lifetime.
     */
    fun registerUser(userId: String, userLocation: String, userPassword: String): Boolean {
        if (inRegistration) {
            val userDocument = userstore.getDocument(USER)
            val (properties, salt) = mapUserAuthData(userId, userLocation, userPassword)
            val (publicKeyEncoded, privateKeyEncoded) = generateEncodedRSAKeyPair()
            properties[USER_PUBLIC_KEY] = publicKeyEncoded.toHex()
            val (encPrivateKey, iv) = encryptBytes(userPassword, salt, privateKeyEncoded)
            properties[USER_SYM_KEY_IV] = iv.toHex()
            properties[ENC_USER_PRIVATE_KEY] = encPrivateKey.toHex()
            userDocument.putProperties(properties)
            inRegistration = false
            return true
        }
        return false
    }

    /**
     * Returns a mapping between basic user property names and attributes together with the user's
     * password salt
     */
    private fun mapUserAuthData(userId: String, userLocation: String, userPassword: String): Pair<HashMap<String, Any>, ByteArray> {
        val properties = HashMap<String, Any>()
        properties[USER_ID] = userId
        properties[USER_LOCATION] = userLocation

        val salt = ByteArray(SALT_LEN)
        SecureRandom().nextBytes(salt)
        properties[USER_SALT] = salt.toHex()

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(userPassword.toByteArray())
        digest.update(salt)
        properties[USER_HASHED_PASS] = digest.digest().toHex()
        return Pair(properties, salt)
    }

    /**
     * Generate a public/private encoded key pair. Public key is encoded under X509 and private
     * key is encoded under PKCS8
     */
    private fun generateEncodedRSAKeyPair(): Pair<ByteArray, ByteArray> {
        val keyGen = KeyPairGenerator.getInstance(SIG_KEYGEN_ALGO)
        keyGen.initialize(4096)
        val keyPair = keyGen.genKeyPair()
        val privateKey = keyPair.private
        val publicKey = keyPair.public
        val x509EncodedKeySpec = X509EncodedKeySpec(publicKey.encoded)
        val pkcs8EncodedKeySpec = PKCS8EncodedKeySpec(privateKey.encoded)
        return Pair(x509EncodedKeySpec.encoded, pkcs8EncodedKeySpec.encoded)
    }

    /**
     * Encrypts the given bytes with a key generated from the provided password and salt.
     * Returns the encrypted bytes as well as the IV used during encryption
     */
    private fun encryptBytes(password: String, salt: ByteArray, bytes: ByteArray): Pair<ByteArray, ByteArray> {
        val secret = getSecretSpec(password, salt)
        val cipher = Cipher.getInstance(ENC_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        val iv = cipher.parameters.getParameterSpec(IvParameterSpec::class.java).iv
        val cipherText = cipher.doFinal(bytes)
        return Pair(cipherText, iv)
    }

    /** Returns a unique encryption key generated from the password and salt */
    private fun getSecretSpec(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(ENC_KEYGEN_ALGO)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LEN)
        return SecretKeySpec(factory.generateSecret(spec).encoded, BASE_ENC_ALGO)
    }

    private fun getBytesFromRawRes(id: Int): ByteArray {
        return IOUtils.toByteArray(applicationResources.openRawResource(id))
    }
}