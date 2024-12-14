package work.on_t.w.apub

import com.google.common.io.BaseEncoding
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest


fun apResolve(plugin: ApPlugin, object_: String): JsonObject {
    val pdc = plugin.server.worlds.first().persistentDataContainer
    val sha256 = MessageDigest.getInstance("SHA-384")
    val key = NamespacedKey(plugin, "cache/${BaseEncoding.base32().omitPadding().encode(sha256.digest())}")

    var cached = pdc.get(key, PersistentDataType.STRING)
    if (cached == null) {
        plugin.logger.info("Resolving AP object: ${object_}")

        val connection = URI(object_).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"")
        cached = connection.inputStream.readAllBytes().decodeToString()

        pdc.set(key, PersistentDataType.STRING, cached)
    }

    return JsonParser.parseString(cached).asJsonObject
}