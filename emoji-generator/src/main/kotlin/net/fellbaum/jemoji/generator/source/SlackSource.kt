package net.fellbaum.jemoji.generator.source

import net.fellbaum.jemoji.generator.jsonMapper
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.html.HtmlPage
import org.htmlunit.html.HtmlScript
import org.jsoup.Connection
import org.jsoup.Jsoup

private const val SLACK_SIGNIN_URL =
    "https://app.slack.com/workspace-signin?redir=%2Fgantry%2Fauth%3Fapp%3Dclient%26lc%3D1724516944%26return_to%3D%252Fclient%26teams%3D"
private val SLACK_EMOJI_SCRIPT_REGEX = Regex("https://a\\.slack-edge\\.com/.*/signin-core\\..*.primer.min.js")
private const val EMOJI_JSON_MARKER = ".exports=JSON.parse('{\"100\""

/**
 * Returns emoji -> Slack aliases (including skin tone variations), scraped from the
 * emoji script loaded by the Slack sign-in page.
 */
fun fetchSlackAliases(): Map<String, List<String>> {
    val node = jsonMapper.readTree(extractEmojiJson(fetchEmojiScript()))

    return buildMap<String, MutableList<String>> {
        node.properties().forEach { (_, emoji) ->
            addTo(hexCodepointsToString(emoji["unicode"].asString(), "-"), colonAlias(emoji["name"].asString()))

            if (emoji.hasNonNull("skinVariations")) {
                emoji["skinVariations"].properties().forEach { (_, variation) ->
                    addTo(
                        hexCodepointsToString(variation["unicode"].asString(), "-"),
                        colonAlias(variation["name"].asString())
                    )
                }
            }
        }
    }
}

private fun fetchEmojiScript(): String {
    val scriptSrc = WebClient(BrowserVersion.FIREFOX).use { webClient ->
        webClient.options.isJavaScriptEnabled = false
        webClient.options.isThrowExceptionOnScriptError = false
        webClient.options.isCssEnabled = false
        webClient.waitForBackgroundJavaScript(10000)
        val page: HtmlPage = webClient.getPage(SLACK_SIGNIN_URL)

        page.getByXPath<HtmlScript>("//script[@src]")
            .map { it.srcAttribute }
            .firstOrNull { it.matches(SLACK_EMOJI_SCRIPT_REGEX) }
            ?: throw IllegalStateException("Emoji script source not found")
    }

    return Jsoup.connect(scriptSrc)
        .ignoreContentType(true)
        .method(Connection.Method.GET)
        .maxBodySize(Integer.MAX_VALUE)
        .execute()
        .body()
}

/** Cuts the `{"100": {...}, ...}` object out of `.exports=JSON.parse('{"100"...}}')},` */
private fun extractEmojiJson(script: String): String {
    val start = script.substring(script.indexOf(EMOJI_JSON_MARKER) + ".exports=JSON.parse('".length)
    return start.take(start.indexOf("\"}}')},") + 3)
}
