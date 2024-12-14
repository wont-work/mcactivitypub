package work.on_t.w.apub

import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.java.JavaPlugin
import work.on_t.w.apub.web.ApHandler
import java.io.FileNotFoundException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec

class ApPlugin : JavaPlugin() {
    val base32 = BaseEncoding.base32().omitPadding()
    val gson = Gson()
    val persistentDataContainer: PersistentDataContainer by lazy { server.worlds.first().persistentDataContainer }
    val root: String by lazy { "https://${host}" }

    lateinit var privateKey: PrivateKey
    lateinit var publicKey: PublicKey
    lateinit var host: String

    private fun loadKeypair() {
        val keyFile = dataFolder.resolve("rsa.key")

        try {
            val key = PKCS8EncodedKeySpec(keyFile.readBytes())
            val kf = KeyFactory.getInstance("RSA")
            val pk = kf.generatePrivate(key) as RSAPrivateCrtKey

            logger.info("Loaded RSA key")
            privateKey = pk
            publicKey = kf.generatePublic(RSAPublicKeySpec(pk.modulus, pk.publicExponent, pk.params))
        } catch (e: FileNotFoundException) {
            val kg = KeyPairGenerator.getInstance("RSA")
            kg.initialize(2048) // lowest size accepted by all instance software
            val kp = kg.genKeyPair()
            keyFile.writeBytes(kp.private.encoded)

            logger.info("Generated RSA key")
            privateKey = kp.private
            publicKey = kp.public
        }
    }

    override fun onEnable() {
        saveDefaultConfig()
        host = config.getString("host")!!

        loadKeypair()
        ApHandler.start(this)
    }
}
