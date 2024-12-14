package work.on_t.w.apub.model

import com.google.gson.annotations.SerializedName

data class Actor(
    @SerializedName("@context")
    val context: Array<String>,
    val id: String,
    val type: String,
    val preferredUsername: String,
    val name: String,
    val inbox: String,
    val publicKey: ActorPublicKey,
    val endpoints: ActorEndpoints
) {
    data class ActorPublicKey(val id: String, val owner: String, val publicKeyPem: String)
    data class ActorEndpoints(val sharedInbox: String)
}