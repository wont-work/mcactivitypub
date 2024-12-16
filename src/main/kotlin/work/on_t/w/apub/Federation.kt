package work.on_t.w.apub

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import work.on_t.w.apub.util.getApFollowers
import work.on_t.w.apub.util.getApId
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.security.Signature
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun apResolve(plugin: ApPlugin, object_: JsonElement, player: Player? = null): JsonObject {
    if (object_.isJsonObject) return object_.asJsonObject
    return apResolve(plugin, object_.asString, player)
}

fun apResolve(plugin: ApPlugin, object_: String, player: Player? = null): JsonObject {
    val sha384 = MessageDigest.getInstance("SHA-384")
    sha384.update(object_.encodeToByteArray())
    val key = NamespacedKey(plugin, "cache/${plugin.base32.encode(sha384.digest())}")

    var cached = plugin.persistentDataContainer.get(key, PersistentDataType.STRING)
    if (cached == null) {
        plugin.logger.info("Resolving AP object: ${object_}")

        val req = URI(object_).toURL().openConnection() as HttpURLConnection
        req.requestMethod = "GET"
        req.setRequestProperty(
            "Accept", "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\""
        )

        var player = player ?: plugin.server.onlinePlayers.firstOrNull()
        if (player != null) httpSign(plugin, req, player)

        cached = req.inputStream.readAllBytes().decodeToString()
        plugin.persistentDataContainer.set(key, PersistentDataType.STRING, cached)
    }

    return JsonParser.parseString(cached).asJsonObject
}

fun apPost(plugin: ApPlugin, player: Player, inbox: String, body: ByteArray) {
    plugin.logger.info("POSTing to inbox: ${inbox}")

    val req = URI(inbox).toURL().openConnection() as HttpURLConnection
    req.requestMethod = "POST"
    req.setRequestProperty(
        "Content-Type", "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\""
    )

    httpSign(plugin, req, player, body)
    req.doOutput = true
    req.outputStream.write(body)

    if (req.responseCode >= 300) plugin.logger.warning("An error occured: ${req.responseMessage}")
}

val httpDateFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"))!!

@OptIn(ExperimentalEncodingApi::class)
fun httpSign(plugin: ApPlugin, req: HttpURLConnection, player: Player, body: ByteArray? = null) {
    val date = LocalDateTime.now().format(httpDateFormatter)

    req.setRequestProperty("Host", req.url.host)
    req.setRequestProperty("Date", date)

    val headers = arrayListOf("(request-target)", "host", "date")
    if (body != null) headers.add("digest")

    val rsa = Signature.getInstance("SHA256withRSA")
    rsa.initSign(plugin.privateKey)
    rsa.update("(request-target): ${req.requestMethod.lowercase()} ${req.url.path}\nhost: ${req.url.host}\ndate: ${date}".encodeToByteArray())
    if (headers.contains("digest")) {
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(body)
        val digest = "SHA-256=${Base64.encode(sha256.digest())}"

        rsa.update("\ndigest: ${digest}".encodeToByteArray())
        req.setRequestProperty("Digest", digest)
    }

    // @formatter:off
    req.setRequestProperty(
        "Signature",
        "keyId=\"${player.getApId(plugin)}#rsa-key\"," +
        "algorithm=\"rsa-sha256\"," +
        "headers=\"${headers.joinToString(" ")}\"," +
        "signature=\"${Base64.encode(rsa.sign())}\""
    )
    // @formatter:on
}

fun apBroadcast(plugin: ApPlugin, player: Player, activity: JsonObject) {
    val followers = player.getApFollowers(plugin)
    if (followers.isEmpty()) return

    // @formatter:off
    val inboxes = followers
        .map { apResolve(plugin, it) }
        .map { it["endpoints"]?.asJsonObject?.get("sharedInbox")?.asString ?: it["inbox"].asString }
        .distinct()
    // @formatter:on

    for (inbox in inboxes) {
        apPost(plugin, player, inbox, plugin.gson.toJson(activity).encodeToByteArray())
    }
}
