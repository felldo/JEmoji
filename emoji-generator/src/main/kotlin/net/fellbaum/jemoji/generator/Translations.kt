package net.fellbaum.jemoji.generator

import net.fellbaum.jemoji.generator.model.GradleEmoji
import net.fellbaum.jemoji.generator.model.LocaleAnnotations

/**
 * Merges CLDR annotations into the emoji list.
 *
 * CLDR only has entries for some qualifications of an emoji (fully-qualified, minimally-qualified, unqualified).
 * [expandedDescriptions] tries to fill the gaps via emojis that share the same English description.
 * Note: the lookup uses the *translated* text as key, so in practice this only fills gaps for `en`
 * (and for translations that happen to equal the English text).
 */
class Translations(emojis: List<GradleEmoji>, val locales: List<LocaleAnnotations>) {

    private val emojisByDescription: Map<String, List<GradleEmoji>> = emojis.groupBy { it.description as String }
    private val descriptionByEmoji: Map<String, String> = emojis.associate { it.emoji to it.description as String }

    /**
     * The locale's descriptions, plus every emoji whose English description equals one of the
     * (translated) descriptions and that has no entry yet.
     */
    fun expandedDescriptions(locale: LocaleAnnotations): Map<String, String> {
        val result = LinkedHashMap(locale.descriptions)
        locale.descriptions.values.forEach { description ->
            emojisByDescription[description]?.forEach { result.putIfAbsent(it.emoji, description) }
        }
        return result
    }

    /** Sets the English CLDR keywords on [emojis], shared by all emojis with the same description. */
    fun withEnglishKeywords(emojis: List<GradleEmoji>): List<GradleEmoji> {
        val english = locales.firstOrNull { it.locale == "en" } ?: return emojis

        val keywordsByEmoji = HashMap<String, Set<String>>()
        english.keywords.forEach { (emoji, keywords) ->
            val description = descriptionByEmoji[emoji] ?: return@forEach
            emojisByDescription.getValue(description).forEach { keywordsByEmoji[it.emoji] = keywords.toSet() }
        }

        return emojis.map { emoji -> keywordsByEmoji[emoji.emoji]?.let { emoji.copy(keywords = it) } ?: emoji }
    }

    /** Replaces description and keywords with maps of locale -> value, as written to `emojis-full.json`. */
    fun withAllLanguages(emojis: List<GradleEmoji>, descriptionsByLocale: Map<String, Map<String, String>>) =
        emojis.map { emoji ->
            emoji.copy(
                description = descriptionsByLocale.mapValues { (_, descriptions) -> descriptions[emoji.emoji] },
                keywords = locales.associate { it.locale to it.keywords[emoji.emoji] },
            )
        }
}
