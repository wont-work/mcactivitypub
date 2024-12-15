package work.on_t.w.apub.web

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.apPost
import work.on_t.w.apub.apResolve
import work.on_t.w.apub.model.Accept
import work.on_t.w.apub.util.getApId
import work.on_t.w.apub.util.updateSavedApFollowerData
import java.net.URI
import java.util.*

class InboxHandler(private val plugin: ApPlugin) {
    fun handle(req: HttpExchange) {
        handleActivity(JsonParser.parseReader(req.requestBody.bufferedReader()).asJsonObject)

        req.sendResponseHeaders(200, 0)
    }

    private fun handleActivity(activity: JsonObject) {
        val id = activity["id"].asString
        val type = activity["type"].asString
        val actor = activity["actor"].asString

        plugin.logger.info("Received $type activity: $id")
        if (actor.startsWith(plugin.root)) return

        if (type == "Follow") {
            val object_ = activity["object"].asString

            val uuidStr = object_.removePrefix("${plugin.root}/players/")
            if (uuidStr == object_) return // prefix didn't exist in string
            val uuid = UUID.fromString(uuidStr)
            val player = plugin.server.getPlayer(uuid) ?: return

            val resolved = apResolve(plugin, actor)
            val resolvedHandle = "${resolved["preferredUsername"].asString}@${URI(actor).authority}"

            val playerId = player.getApId(plugin)
            val response = plugin.gson.toJson(
                Accept(
                    context = arrayOf("https://www.w3.org/ns/activitystreams"),
                    id = "${playerId}#accept/${System.currentTimeMillis()}",
                    type = "Accept",
                    actor = playerId,
                    object_ = activity
                )
            )
            apPost(plugin, player, resolved["inbox"].asString, response.encodeToByteArray())

            player.updateSavedApFollowerData(plugin) { it.add(actor) }
            player.sendMessage("$resolvedHandle is now following you!")
        } else if (type == "Undo") {
            val inner = activity["object"].asJsonObject
            val innerType = inner["type"].asString

            if (innerType == "Follow") {
                val object_ = inner["object"].asString
                val uuidStr = object_.removePrefix("${plugin.root}/players/")
                if (uuidStr == object_) return // prefix didn't exist in string
                val uuid = UUID.fromString(uuidStr)
                val player = plugin.server.getPlayer(uuid) ?: return

                val resolved = apResolve(plugin, actor)
                val resolvedHandle = "${resolved["preferredUsername"].asString}@${URI(actor).authority}"

                player.updateSavedApFollowerData(plugin) { it.remove(actor) }
                player.sendMessage("$resolvedHandle is no longer following you")
            }
        } else if (type == "Create") {
            val object_ = apResolve(plugin, activity["object"])
            val actor = apResolve(plugin, object_["attributedTo"])
            val actorId = actor["id"].asString
            val content = object_["content"].asString

            // @formatter:off
            plugin.server.broadcast(
                Component.text("<")
                    .append(Component.text(actor["preferredUsername"].asString)
                            .append(Component.text("@${URI(actorId).authority}")
                                .color(TextColor.color(0xBEBEBE)))
                            .clickEvent(ClickEvent.openUrl(actorId)))
                    .append(Component.text("> ")
                        .append(Component.text(content)))
            )
            // @formatter:on
        }
    }
}