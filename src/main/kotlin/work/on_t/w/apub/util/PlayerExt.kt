package work.on_t.w.apub.util

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import work.on_t.w.apub.ApPlugin

fun Player.getApId(plugin: ApPlugin) = "https://${plugin.host}/players/${this.uniqueId}"

fun Player.updateSavedApFollowerData(plugin: ApPlugin, lambda: (HashSet<String>) -> Unit) {
    val key = NamespacedKey(plugin, "followers")
    val followers = this.persistentDataContainer.getOrDefault(key, PersistentDataType.STRING, "").split(',').toHashSet()
    lambda(followers)
    this.persistentDataContainer.set(key, PersistentDataType.STRING, followers.joinToString(","))
}