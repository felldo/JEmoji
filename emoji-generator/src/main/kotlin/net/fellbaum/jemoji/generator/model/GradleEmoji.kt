package net.fellbaum.jemoji.generator.model

/**
 * One emoji as written to `emojis.json`. The field names and order are the JSON format
 * that jemoji deserializes at runtime, so don't rename or reorder them.
 */
data class GradleEmoji(
    val emoji: String,
    val unicode: String,
    val htmlDec: String,
    val htmlHex: String,
    val urlEncoded: String,
    val discordAliases: Set<String>,
    val githubAliases: Set<String>,
    val slackAliases: Set<String>,
    val hasFitzpatrick: Boolean,
    val hasHairStyle: Boolean,
    val version: Double,
    val qualification: String,
    /** English description (`String`), or `Map<locale, String?>` in `emojis-full.json`. */
    val description: Any,
    val group: String,
    val subgroup: String,
    val hasVariationSelectors: Boolean,
    /** English keywords (`Set<String>`), or `Map<locale, List<String>?>` in `emojis-full.json`. */
    val keywords: Any
)
