package work.on_t.w.apub.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.apBroadcast
import work.on_t.w.apub.model.Create
import work.on_t.w.apub.model.Note
import work.on_t.w.apub.util.getApId
import java.time.Instant

class ChatListener(private val plugin: ApPlugin) : Listener {
    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {
        val playerId = event.player.getApId(plugin)
        val activity = Create(
            context = arrayOf("https://www.w3.org/ns/activitystreams"),
            id = "${playerId}#accept/${System.currentTimeMillis()}",
            type = "Create",
            actor = playerId,
            object_ = Note(
                context = arrayOf("https://www.w3.org/ns/activitystreams"),
                id = "${playerId}/note/${System.currentTimeMillis()}",
                type = "Note",
                to = arrayOf("https://www.w3.org/ns/activitystreams#Public"),
                attributedTo = playerId,
                content = PlainTextComponentSerializer.plainText().serialize(event.message()),
                published = Instant.now().toString()
            )
        )

        apBroadcast(plugin, event.player, plugin.gson.toJsonTree(activity).asJsonObject)
    }
}