package work.on_t.w.apub.model

import com.google.gson.annotations.SerializedName
import java.time.Instant

data class Note(
    @SerializedName("@context") val context: Array<String>,
    val id: String,
    val type: String,
    val to: Array<String>,
    val attributedTo: String,
    val content: String,
    val published: String
)