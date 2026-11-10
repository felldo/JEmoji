package net.fellbaum.jemoji.generator.model

/** Emoji string -> aliases (e.g. `:thumbsup:`), per platform. */
data class PlatformAliases(
    val discord: Map<String, List<String>>,
    val github: Map<String, List<String>>,
    val slack: Map<String, List<String>>,
)

/** CLDR annotations of one locale, keyed by emoji string. */
data class LocaleAnnotations(
    val locale: String,
    val descriptions: Map<String, String>,
    val keywords: Map<String, List<String>>,
)
