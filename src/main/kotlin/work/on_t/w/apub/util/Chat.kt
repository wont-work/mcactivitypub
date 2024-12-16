package work.on_t.w.apub.util

import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import java.net.URI

fun renderHandle(actor: JsonObject): TextComponent {
    val actorId = actor["id"].asString
    val actorUrl = actor["url"]?.asString ?: actorId

    return Component.text(actor["preferredUsername"].asString)
        .append(
            Component.text("@${URI(actorId).authority}")
            .color(TextColor.color(0xBEBEBE)))
        .clickEvent(ClickEvent.openUrl(actorUrl))
}

