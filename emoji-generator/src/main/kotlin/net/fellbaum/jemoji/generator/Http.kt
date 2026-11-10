package net.fellbaum.jemoji.generator

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Optional GitHub token from the `GITHUB_TOKEN` environment variable.
 *
 * Without a token, the GitHub API allows 60 requests per hour, which a few local runs of the generator
 * already exhaust. With a token, the limit is 5,000 per hour. Any token works, it needs no scopes
 * (e.g. `GITHUB_TOKEN=$(gh auth token) ./gradlew generate`).
 */
object GitHubToken {
    val value: String? = System.getenv("GITHUB_TOKEN")?.trim()?.takeIf { it.isNotEmpty() }

    fun describe() = if (value != null) "GitHub API: using GITHUB_TOKEN" else
        "GitHub API: no GITHUB_TOKEN set, unauthenticated (60 requests/hour)"
}

/** Minimal shared HTTP client for all sources that don't need a browser. */
class Http(private val client: OkHttpClient = OkHttpClient()) {

    fun get(url: String, vararg headers: Pair<String, String>): String {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> addHeader(name, value) }
            // Only send the token to the API, not to other hosts
            if (url.toHttpUrl().host == "api.github.com") {
                GitHubToken.value?.let { addHeader("Authorization", "Bearer $it") }
            }
        }.build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "GET $url failed with HTTP ${response.code}" }
            return response.body.string()
        }
    }
}
