package net.fellbaum.jemoji.generator.source

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.fellbaum.jemoji.generator.jsonMapper
import org.jsoup.Connection
import org.jsoup.Jsoup
import tools.jackson.module.kotlin.readValue

@JsonIgnoreProperties(ignoreUnknown = true)
private data class DiscordEmoji(
    val names: List<String>,
    val surrogates: String,
    val diversityChildren: List<Int>?,
    val hasDiversityParent: Boolean?,
    val diversity: List<String>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class DiscordEmojiDefinitions(
    val emojis: List<DiscordEmoji>,
    val nameToEmoji: Map<String, Int>,
)

private const val DISCORD_URL = "https://discord.com"
private const val EMOJI_DEFINITIONS_MARKER = "exports=JSON.parse('{\"emojis\":[{\""
private const val ABBREVIATIONS_MARKER = "132565(e){\"use strict\";e.exports=JSON.parse('{\""

/**
 * Returns emoji -> Discord aliases, scraped from the Discord web client's JS assets. Contains:
 * - the emoji names (`:thumbsup:`)
 * - for emojis with one skin tone: the parent's names plus the skin tone alias (`:thumbsup::skin-tone-1:`).
 *   Emojis with several skin tones can't be written this way, Discord renders them as two separate emojis.
 * - abbreviations (`:)`, `>:(`)
 */
fun fetchDiscordAliases(): Map<String, List<String>> {
    val srcFiles = Jsoup.connect("$DISCORD_URL/channels/@me").userAgent("Mozilla").get()
        .select("script[src]").map { it.attr("src") }

    // Discord ships the emoji definitions and the kaomoji abbreviations in two separate, predictably
    // named asset chunks, so there's no need to download and scan every asset on the page for them.
    val emojiDefinitionsFile = srcFiles.firstOrNull { it.matches(Regex("""/assets/vnd-emoji\.\w+\.js""")) }
        ?: throw IllegalStateException("vnd-emoji asset script not found")
    val abbreviationsFile = srcFiles.firstOrNull { it.matches(Regex("""/assets/web\.\w+\.js""")) }
        ?: throw IllegalStateException("web asset script not found")

    val emojiDefinitionsSource = fetchAsset(emojiDefinitionsFile)
    val abbreviationsSource = fetchAsset(abbreviationsFile)

    check(emojiDefinitionsSource.contains("{\"emojis\":")) { "Emoji definitions script source not found" }
    check(abbreviationsSource.contains(ABBREVIATIONS_MARKER)) { "Emoji abbreviations script source not found" }

    val definitions = jsonMapper.readValue<DiscordEmojiDefinitions>(
        extractJsonParseStringLiteral(emojiDefinitionsSource, EMOJI_DEFINITIONS_MARKER)
            .replace("\\\\", "\\")
            .replace(Regex("\\\\x([0-9A-Fa-f]{2})"), "\\\\u00$1")
    )

    // {">:(":"angry",">:-(":"angry",...} -> {"angry": [">:(", ">:-(", ...], ...}
    val abbreviationsByName = buildMap<String, MutableList<String>> {
        jsonMapper.readTree(
            extractJsonParseStringLiteral(abbreviationsSource, ABBREVIATIONS_MARKER)
                .replace("\\\\", "\\")
                .replace("\\'", "'")
        ).properties().forEach { addTo(it.value.asString(), it.key) }
    }

    val additionalNamesByIndex = definitions.nameToEmoji.entries.groupBy { it.value }

    return buildMap<String, MutableList<String>> {
        definitions.emojis.forEachIndexed { index, emoji ->
            val aliases = emoji.names.map(::colonAlias).toMutableList()
            put(emoji.surrogates, aliases)

            additionalNamesByIndex[index]?.let { entries -> aliases.addAll(entries.map { colonAlias(it.key) }) }

            if (emoji.diversity?.size == 1) {
                definitions.emojis.find { it.diversityChildren?.contains(index) == true }?.let { parent ->
                    aliases.addAll(parent.names.map { colonAlias(it) + skinToneAlias(emoji.diversity[0]) })
                }
            }
        }

        abbreviationsByName.forEach { (name, abbreviations) ->
            definitions.emojis.find { it.names.contains(name) }?.let { get(it.surrogates)!!.addAll(abbreviations) }
        }
    }
}

private fun skinToneAlias(diversity: String) = when (diversity) {
    "1f3fb" -> ":skin-tone-1:"
    "1f3fc" -> ":skin-tone-2:"
    "1f3fd" -> ":skin-tone-3:"
    "1f3fe" -> ":skin-tone-4:"
    "1f3ff" -> ":skin-tone-5:"
    else -> throw IllegalStateException(diversity)
}

private fun fetchAsset(path: String): String = Jsoup.connect("$DISCORD_URL$path")
    .ignoreContentType(true)
    .method(Connection.Method.GET)
    .maxBodySize(Integer.MAX_VALUE)
    .execute()
    .body()

/**
 * Extracts the raw (still JS-escaped) contents of a `JSON.parse('...')` single-quoted string literal
 * that starts with [markerWithOpeningQuote] (a prefix ending in the literal opening `'`).
 *
 * A plain `indexOf("}')},")` terminator is not reliable across webpack chunks: whether the JSON.parse
 * call is followed by a comma (another module in the same chunk) or immediately closes the chunk
 * varies, so the closing quote is found by scanning with backslash-escape awareness instead.
 */
private fun extractJsonParseStringLiteral(source: String, markerWithOpeningQuote: String): String {
    val markerIndex = source.indexOf(markerWithOpeningQuote)
    check(markerIndex >= 0) { "Marker not found: $markerWithOpeningQuote" }
    val openQuoteIndex = markerIndex + markerWithOpeningQuote.indexOf('\'')

    var i = openQuoteIndex + 1
    while (true) {
        when (source[i]) {
            '\\' -> i += 2
            '\'' -> return source.substring(openQuoteIndex + 1, i)
            else -> i++
        }
    }
}
