package work.on_t.w.apub.web

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.apResolve
import work.on_t.w.apub.util.updateSavedApFollowerData
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
        val actor = activity["actor"].asString

        plugin.logger.info("Received $type activity: $id")
        if (actor.startsWith(root)) return

        if (type == "Follow") {
            val object_ = activity["object"].asString

            val uuidStr = object_.removePrefix("${root}/players/")
            if (uuidStr == object_) return // prefix didn't exist in string
            val uuid = UUID.fromString(uuidStr)

            val player = plugin.server.onlinePlayers.find { it.uniqueId == uuid }
            if (player == null) return

            val resolved = apResolve(plugin, actor)
            val resolvedHandle = "${resolved["preferredUsername"].asString}@${URI(actor).authority}"

            player.updateSavedApFollowerData(plugin) { it.add(actor) }
            player.sendMessage("$resolvedHandle is now following you!")
        } else if (type == "Undo") {
            val inner = activity["object"].asJsonObject
            val innerType = inner["type"].asString

            if (innerType == "Follow") {
                val object_ = inner["object"].asString
                val uuidStr = object_.removePrefix("${root}/players/")
                if (uuidStr == object_) return // prefix didn't exist in string
                val uuid = UUID.fromString(uuidStr)

                val player = plugin.server.onlinePlayers.find { it.uniqueId == uuid }
                if (player == null) return

                val resolved = apResolve(plugin, actor)
                val resolvedHandle = "${resolved["preferredUsername"].asString}@${URI(actor).authority}"

                player.updateSavedApFollowerData(plugin) { it.remove(actor) }
                player.sendMessage("$resolvedHandle is no longer following you")
            }
        }
    }
}