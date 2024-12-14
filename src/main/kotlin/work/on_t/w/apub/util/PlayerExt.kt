package work.on_t.w.apub.util

import org.bukkit.entity.Player
import work.on_t.w.apub.ApPlugin

fun Player.getApId(plugin: ApPlugin) = "https://${plugin.host}/players/${this.uniqueId}"
