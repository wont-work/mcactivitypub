package work.on_t.w.apub.web

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import work.on_t.w.apub.ApPlugin
import java.net.InetSocketAddress

class ApHandler private constructor(plugin: ApPlugin) : HttpHandler {
    private val webfingerHandler = WebfingerHandler(plugin)
    private val actorHandler = ActorHandler(plugin)
    private val inboxHandler = InboxHandler(plugin)

    companion object {
        fun start(plugin: ApPlugin) {
            val port = plugin.config.getInt("port")
            val server = HttpServer.create(InetSocketAddress(port), 0)
            server.createContext("/", ApHandler(plugin))
            server.executor = null
            server.start()

            plugin.logger.info("Started web server on port :${port} (https://${plugin.host})")
        }
    }

    override fun handle(req: HttpExchange) {
        try {
            if (req.requestMethod == "GET" && req.requestURI.path == "/.well-known/webfinger") {
                webfingerHandler.handle(req)
            } else if (req.requestMethod == "GET" && req.requestURI.path.startsWith("/players/")) {
                actorHandler.handle(req)
            } else if (req.requestMethod == "POST") {
                inboxHandler.handle(req)
            } else {
                req.sendResponseHeaders(404, 0)
            }
        } catch (exc: Exception) {
            exc.printStackTrace()
            val trace = exc.stackTraceToString().encodeToByteArray()

            req.responseHeaders.set("content-type", "text/plain; charset=utf-8")
            req.sendResponseHeaders(500, trace.size.toLong())
            req.responseBody.write(trace)
        }

        req.responseBody.close()
    }
}