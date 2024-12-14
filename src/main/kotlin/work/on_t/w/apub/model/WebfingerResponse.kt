package work.on_t.w.apub.model

data class WebfingerResponse(val subject: String, val links: Array<WebfingerLink>) {
    data class WebfingerLink(val rel: String, val type: String, val href: String)
}