package net.fellbaum.jemoji.generator

import okhttp3.OkHttpClient
import okhttp3.Request

/** Minimal shared HTTP client for all sources that don't need a browser. */
class Http(private val client: OkHttpClient = OkHttpClient()) {

    fun get(url: String, vararg headers: Pair<String, String>): String {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> addHeader(name, value) }
        }.build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "GET $url failed with HTTP ${response.code}" }
            return response.body.string()
        }
    }
}
