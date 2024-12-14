package work.on_t.w.apub.util

import java.security.PublicKey
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun PublicKey.toPem() = Base64.encode(this.encoded).chunked(64)
    .joinToString("\n", "-----BEGIN PUBLIC KEY-----\n", "\n-----END PUBLIC KEY-----\n")
