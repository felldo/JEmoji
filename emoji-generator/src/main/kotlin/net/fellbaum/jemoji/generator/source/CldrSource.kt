package net.fellbaum.jemoji.generator.source

import net.fellbaum.jemoji.generator.Http
import net.fellbaum.jemoji.generator.jsonMapper
import net.fellbaum.jemoji.generator.model.LocaleAnnotations
import net.fellbaum.jemoji.generator.writeTextCreatingParents
import tools.jackson.module.kotlin.readValue
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private const val CLDR_LOCALES_API_URL =
    "https://api.github.com/repos/unicode-org/cldr-json/contents/cldr-json/cldr-annotations-derived-full/annotationsDerived"
private const val CLDR_RAW_URL = "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json"
private val GITHUB_RAW_ACCEPT_HEADER = "Accept" to "application/vnd.github.raw+json"

/**
 * Emoji descriptions ("tts") and keywords ("default") per locale from the `unicode-org/cldr-json` repository.
 * The raw files are cached in [rawFilesDir].
 */
class CldrSource(private val http: Http, private val rawFilesDir: File) {

    /** Fetches all locales in parallel. The result is in the order of the CLDR directory listing. */
    fun fetchAllLocales(): List<LocaleAnnotations> {
        val locales = fetchLocales()
        val processed = AtomicInteger(0)
        return locales.parallelStream().map { locale ->
            val index = processed.getAndIncrement()
            if (index % 10 == 0) {
                println("$index / ${locales.size} description files processed")
            }
            fetchLocale(locale)
        }.toList()
    }

    private fun fetchLocales(): List<String> =
        jsonMapper.readValue<List<Map<String, Any>>>(http.get(CLDR_LOCALES_API_URL, GITHUB_RAW_ACCEPT_HEADER))
            .map { it["name"] as String }

    /**
     * Merges the derived annotations (e.g. skin tone variants) and the regular annotations of [locale].
     * The regular annotations come second and win on conflicts.
     */
    private fun fetchLocale(locale: String): LocaleAnnotations {
        val descriptions = LinkedHashMap<String, String>()
        val keywords = LinkedHashMap<String, List<String>>()

        listOf(
            "$CLDR_RAW_URL/cldr-annotations-derived-full/annotationsDerived/$locale/annotations.json",
            "$CLDR_RAW_URL/cldr-annotations-full/annotations/$locale/annotations.json",
        ).forEach { url ->
            val fileContent = jsonMapper.readTree(http.get(url, GITHUB_RAW_ACCEPT_HEADER))

            val isDerived = fileContent.has("annotationsDerived")
            rawFilesDir.resolve("$locale${if (isDerived) "-derieved" else ""}.json")
                .writeTextCreatingParents(jsonMapper.writeValueAsString(fileContent))

            val root = fileContent["annotations"] ?: fileContent["annotationsDerived"]
            val annotations = root?.get("annotations") ?: return@forEach

            annotations.properties().forEach { (emoji, annotation) ->
                if (annotation.has("tts")) {
                    descriptions[emoji] = annotation["tts"].joinToString(" ") { it.asString() }
                    keywords[emoji] = annotation["default"]?.values()?.map { it.asString() }.orEmpty()
                }
            }
        }

        return LocaleAnnotations(locale, descriptions, keywords)
    }
}
