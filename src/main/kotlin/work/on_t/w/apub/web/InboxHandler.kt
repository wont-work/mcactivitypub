package work.on_t.w.apub.web

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import net.kyori.adventure.text.Component
import org.bukkit.scheduler.BukkitRunnable
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.apPost
import work.on_t.w.apub.apResolve
import work.on_t.w.apub.model.Activity
import work.on_t.w.apub.util.getApId
import work.on_t.w.apub.util.renderHandle
import work.on_t.w.apub.util.updateSavedApFollowerData
import work.on_t.w.apub.util.updateSavedApFollowingData
import java.util.*

// janky hack to clean up all html tags from content
val htmlTagRegex = Regex("<.*?>")

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

            val playerId = player.getApId(plugin)
            val response = plugin.gson.toJson(
                Activity(
                    context = arrayOf("https://www.w3.org/ns/activitystreams"),
                    id = "${playerId}/accept/${System.currentTimeMillis()}",
                    to = arrayOf(actor),
                    type = "Accept",
                    actor = playerId,
                    object_ = activity
                )
            )
            apPost(plugin, player, resolved["inbox"].asString, response.encodeToByteArray())

            player.updateSavedApFollowerData(plugin) { it.add(actor) }
            player.sendMessage(renderHandle(resolved).append(Component.text(" is now following you")))
        } else if (type == "Accept") {
            val inner = activity["object"].asJsonObject
            val innerType = inner["type"].asString

            if (innerType == "Follow") {
                val object_ = inner["actor"].asString
                val uuidStr = object_.removePrefix("${plugin.root}/players/")
                if (uuidStr == object_) return // prefix didn't exist in string
                val uuid = UUID.fromString(uuidStr)
                val player = plugin.server.getPlayer(uuid) ?: return
                val resolved = apResolve(plugin, actor)

                player.updateSavedApFollowingData(plugin) { it.add(actor) }
                player.sendMessage(renderHandle(resolved).append(Component.text(" accepted your follow request")))
            }
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

                player.updateSavedApFollowerData(plugin) { it.remove(actor) }
                player.sendMessage(renderHandle(resolved).append(Component.text(" is no longer following you")))
            }
        } else if (type == "Create") {
            val object_ = apResolve(plugin, activity["object"])
            val actor = apResolve(plugin, object_["attributedTo"])
            val content = object_["content"].asString.replace(htmlTagRegex, "")

            // @formatter:off
            plugin.server.broadcast(
                Component.text("<")
                    .append(renderHandle(actor))
                    .append(Component.text("> "))
                    .append(Component.text(content))
            )
            // @formatter:on
        } else if (type == "Bite") {
            val target = activity["to"].asJsonArray.firstOrNull()?.asString ?: return

            val (uuidStr) = target.removePrefix("${plugin.root}/players/").split('/', limit = 2)
            if (uuidStr == target) return // prefix didn't exist in string
            val uuid = UUID.fromString(uuidStr)
            val player = plugin.server.getPlayer(uuid) ?: return
            val resolved = apResolve(plugin, actor)

            object : BukkitRunnable() {
                override fun run() {
                    player.damage(1.0)
                    player.sendMessage(renderHandle(resolved).append(Component.text(" bit you")))
                }
            }.runTask(plugin)
        }
    }
}