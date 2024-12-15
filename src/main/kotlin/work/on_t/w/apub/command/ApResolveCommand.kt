package work.on_t.w.apub.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.apResolve

class ApResolveCommand(private val plugin: ApPlugin) : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<String>) {
        if (args.size != 1) {
            source.sender.sendMessage("/apresolve <uri>")
            return
        }

        val obj = apResolve(plugin, args.first(), source.sender as? Player)
        source.sender.sendMessage(obj.toString())
    }
}