package work.on_t.w.apub

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

fun apResolve(plugin: ApPlugin, object_: String): JsonObject {
    val sha384 = MessageDigest.getInstance("SHA-384")
    sha384.update(object_.encodeToByteArray())
    val key = NamespacedKey(plugin, "cache/${plugin.base32.encode(sha384.digest())}")

    var cached = plugin.persistentDataContainer.get(key, PersistentDataType.STRING)
    if (cached == null) {
        plugin.logger.info("Resolving AP object: ${object_}")

        val connection = URI(object_).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty(
            "Accept",
            "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\""
        )
        cached = connection.inputStream.readAllBytes().decodeToString()

        plugin.persistentDataContainer.set(key, PersistentDataType.STRING, cached)
    }

    return JsonParser.parseString(cached).asJsonObject
}