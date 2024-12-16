package work.on_t.w.apub.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.apPost
import work.on_t.w.apub.apResolve
import work.on_t.w.apub.model.Activity
import work.on_t.w.apub.util.getApId
import work.on_t.w.apub.util.renderHandle
import work.on_t.w.apub.webfingerResolve

class ApFollowCommand(private val plugin: ApPlugin) : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val player = source.sender as? Player
        if (player == null) {
            source.sender.sendMessage("Only players can follow")
            return
        }

        if (args.size != 1) {
            source.sender.sendMessage("/apfollow <@username@server>")
            return
        }

        val id = webfingerResolve(plugin, args.first())
        if (id == null) {
            source.sender.sendMessage("Could not resolve handle")
            return
        }

        val actor = apResolve(plugin, id, player)

        val playerId = player.getApId(plugin)
        val response = plugin.gson.toJson(
            Activity(
                context = arrayOf("https://www.w3.org/ns/activitystreams"),
                id = "${playerId}/follow/${System.currentTimeMillis()}",
                to = arrayOf(actor["id"].asString),
                type = "Follow",
                actor = playerId,
                object_ = actor["id"]
            )
        )

        apPost(plugin, player, actor["inbox"].asString, response.encodeToByteArray())
        source.sender.sendMessage(Component.text("Follow request sent to ").append(renderHandle(actor)))
    }
}