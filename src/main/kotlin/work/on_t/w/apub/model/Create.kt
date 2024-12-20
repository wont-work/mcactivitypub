package work.on_t.w.apub.model

import com.google.gson.annotations.SerializedName

data class Create(
    @SerializedName("@context") val context: Array<String>,
    val id: String,
    val type: String,
    val actor: String,
    @SerializedName("object") val object_: Note
)