package net.fellbaum.jemoji.generator.model

data class GradleEmoji(
    val emoji: String,
    val unicode: String,
    val htmlDec: String,
    val htmlHex: String,
    val urlEncoded: String,
    val discordAliases: Set<String>,
    val githubAliases: Set<String>,
    val slackAliases: Set<String>,
    var hasFitzpatrick: Boolean,
    var hasHairStyle: Boolean,
    val version: Double,
    val qualification: String,
    var description: Any,
    val group: String,
    val subgroup: String,
    val hasVariationSelectors: Boolean,
    var keywords: Any
)
