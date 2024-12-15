package work.on_t.w.apub.web

import com.sun.net.httpserver.HttpExchange
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.model.Actor
import work.on_t.w.apub.util.getApId
import work.on_t.w.apub.util.toPem
import java.util.*

class ActorHandler(private val plugin: ApPlugin) {
    fun handle(req: HttpExchange) {
        val (uuidStr, _) = req.requestURI.path.removePrefix("/players/").split('/', limit = 2)
        plugin.logger.info("Received GET for player $uuidStr")

        val uuid = UUID.fromString(uuidStr)
        val player = plugin.server.getPlayer(uuid)
        if (player == null) {
            req.sendResponseHeaders(404, 0)
            return
        }

        val id = player.getApId(plugin)
        val response = plugin.gson.toJson(
            Actor(
                context = arrayOf("https://www.w3.org/ns/activitystreams", "https://w3id.org/security/v1"),
                id = id,
                type = "Person",
                preferredUsername = player.name,
                name = player.displayName,
                inbox = "${plugin.root}/inbox",
                publicKey = Actor.ActorPublicKey(
                    id = "${id}#rsa-key", owner = id, publicKeyPem = plugin.publicKey.toPem()
                ),
                endpoints = Actor.ActorEndpoints(sharedInbox = "${plugin.root}/inbox")
            )
        )

        req.responseHeaders.set("content-type", "application/activity+json; charset=utf-8")
        req.sendResponseHeaders(200, response.length.toLong())
        req.responseBody.write(response.encodeToByteArray())
    }
}