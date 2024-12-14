package work.on_t.w.apub.web

import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import work.on_t.w.apub.ApPlugin
import work.on_t.w.apub.model.WebfingerResponse
import work.on_t.w.apub.util.getApId


class WebfingerHandler(val plugin: ApPlugin) {
    val gson = Gson()

    fun handle(req: HttpExchange) {
        val resource = req.requestURI.query.removePrefix("resource=")

        val (handle, _) = resource.removePrefix("acct:").split('@', limit = 2)
        val player = plugin.server.onlinePlayers.find { it.name.equals(handle, ignoreCase = true) }
        if (player == null) {
            req.sendResponseHeaders(404, 0)
            return
        }

        val response = gson.toJson(
            WebfingerResponse(
                subject = "acct:${player.name}@${plugin.host}", links = arrayOf(
                    WebfingerResponse.WebfingerLink(
                        rel = "self",
                        type = "application/activity+json",
                        href = player.getApId(plugin)
                    )
                )
            )
        )

        req.responseHeaders.set("content-type", "application/jrd+json; charset=utf-8")
        req.sendResponseHeaders(200, response.length.toLong())
        req.responseBody.write(response.encodeToByteArray())
    }
}