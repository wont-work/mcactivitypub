package work.on_t.w.apub.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class Activity(
    @SerializedName("@context") val context: Array<String>,
    val id: String,
    val to: Array<String>,
    val type: String,
    val actor: String,
    @SerializedName("object") val object_: JsonElement
)