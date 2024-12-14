package work.on_t.w.apub.web

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.apResolve
import java.net.URI
import java.util.UUID

class InboxHandler(private val plugin: ApPlugin) {
    fun handle(req: HttpExchange) {
        handleActivity(JsonParser.parseReader(req.requestBody.bufferedReader()).asJsonObject)

        req.sendResponseHeaders(200, 0)
    }

    private fun handleActivity(activity: JsonObject) {
        val root = "https://${plugin.host}"

        val id = activity["id"].asString
        val type = activity["type"].asString

        plugin.logger.info("Received ${type} activity: ${id}")
        if (type == "Follow") {
            val actor = activity["actor"].asString
            val object_ = activity["object"].asString

            val uuidStr = object_.removePrefix("${root}/players/")
            if (uuidStr == object_) return // prefix didn't exist in string
            val uuid = UUID.fromString(uuidStr)

            val player = plugin.server.onlinePlayers.find { it.uniqueId == uuid }
            if (player == null) return

            val resolved = apResolve(plugin, actor)
            val resolvedHandle = "${resolved["preferredUsername"].asString}@${URI(actor).authority}"

            val followersKey = NamespacedKey(plugin, "followers")
            val followers = player.persistentDataContainer.getOrDefault(followersKey, PersistentDataType.STRING, "").split(',').toHashSet()
            followers.add(actor)
            player.persistentDataContainer.set(followersKey, PersistentDataType.STRING, followers.joinToString(","))
            player.sendMessage("${resolvedHandle} is now following you!")
        }
    }
}