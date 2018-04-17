import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ThreadLocalRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val SALT_LEN = 8
private const val ITERATION_COUNT = 65536
private const val KEY_LEN = 256
private const val ASYM_ALGORITHM = "RSA"
private const val PATH_TO_RAW_RES = "app/src/main/res/raw/"

fun main(args: Array<String>) {
    val password = args[0]

    val salt = ByteArray(SALT_LEN)
    SecureRandom().nextBytes(salt)
    val saltFile = FileOutputStream("${PATH_TO_RAW_RES}ca_salt")
    saltFile.write(salt)
    saltFile.close()

    val hashedPassFile = FileOutputStream("${PATH_TO_RAW_RES}ca_hashed_password")
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(password.toByteArray())
    digest.update(salt)
    hashedPassFile.write(digest.digest())
    hashedPassFile.close()

    val factory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1")
    val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LEN)
    val secret = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secret)
    val iv = cipher.parameters.getParameterSpec(IvParameterSpec::class.java).iv
    val symKeyIvFile = FileOutputStream("${PATH_TO_RAW_RES}ca_sym_key_iv")
    symKeyIvFile.write(iv)
    symKeyIvFile.close()

    val keyGen = KeyPairGenerator.getInstance(ASYM_ALGORITHM)
    keyGen.initialize(4096)
    val keyPair = keyGen.genKeyPair()
    val privateKey = keyPair.private
    val publicKey = keyPair.public

    val x509EncodedKeySpec = X509EncodedKeySpec(publicKey.encoded)
    val certAuthPubKeyFile = FileOutputStream("${PATH_TO_RAW_RES}ca_public_key")
    certAuthPubKeyFile.write(x509EncodedKeySpec.encoded)
    certAuthPubKeyFile.close()

    val pkcs8EncodedKeySpec = PKCS8EncodedKeySpec(privateKey.encoded)
    val certAuthEncPrivateKeyFile = FileOutputStream("${PATH_TO_RAW_RES}enc_ca_private_key")
    val cipherText = cipher.doFinal(pkcs8EncodedKeySpec.encoded)
    certAuthEncPrivateKeyFile.write(cipherText)
    certAuthEncPrivateKeyFile.close()

}
