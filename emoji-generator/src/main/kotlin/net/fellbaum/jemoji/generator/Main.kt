package net.fellbaum.jemoji.generator

import net.fellbaum.jemoji.generator.codegen.generateEmojiGroupEnum
import net.fellbaum.jemoji.generator.codegen.generateEmojiLanguageEnum
import net.fellbaum.jemoji.generator.codegen.generateEmojiSubGroupEnum
import net.fellbaum.jemoji.generator.codegen.generateEmojisInterfaces
import net.fellbaum.jemoji.generator.codegen.generatePreComputedConstants
import net.fellbaum.jemoji.generator.model.GradleEmoji
import net.fellbaum.jemoji.generator.model.PlatformAliases
import net.fellbaum.jemoji.generator.source.CldrSource
import net.fellbaum.jemoji.generator.source.UnicodeSource
import net.fellbaum.jemoji.generator.source.fetchDiscordAliases
import net.fellbaum.jemoji.generator.source.fetchGitHubAliases
import net.fellbaum.jemoji.generator.source.fetchSlackAliases
import net.fellbaum.jemoji.generator.source.parseEmojiTestFile
import net.fellbaum.jemoji.generator.source.parseVariationSequences
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.readValue
import java.io.File

/**
 * Generates all emoji data of the project.
 *
 * Arguments:
 * - `--rootDir=<path>`: repository root (default: working directory)
 * - `--all`: also write all per-language files (jemoji-languages resources, `public/description`,
 *   `public/keywords`, `public/emojis-full.json`)
 */
fun main(args: Array<String>) {
    val rootDir = args.firstOrNull { it.startsWith("--rootDir=") }
        ?.removePrefix("--rootDir=")
        ?: System.getProperty("user.dir")
    generate(GeneratorPaths(File(rootDir)), generateAll = args.contains("--all"))
}

fun generate(paths: GeneratorPaths, generateAll: Boolean = false) {
    val http = Http()

    // 1. Fetch the emoji list and enrich it with platform aliases and English CLDR keywords
    val aliases = fetchPlatformAliases(paths, http)
    val parsedEmojis = parseEmojiTestFile(
        UnicodeSource.fetchEmojiTestFile(),
        parseVariationSequences(UnicodeSource.fetchVariationSequencesFile()),
        aliases,
    )
    val translations = Translations(parsedEmojis, CldrSource(http, paths.cldrRawFilesDir).fetchAllLocales())
    val emojis = translations.withEnglishKeywords(parsedEmojis)
    val descriptionsByLocale = translations.locales.associate { it.locale to translations.expandedDescriptions(it) }

    // 2. Write the emoji data files
    paths.emojisResourceFile.writeTextIfChanged(sortedJsonMapper.writeValueAsString(emojis))
    writePublicJson(paths, "emojis", emojis)
    if (generateAll) {
        writeLanguageFiles(paths, emojis, translations, descriptionsByLocale)
    }

    // 3. Generate the Java sources
    generateEmojiLanguageEnum(translations.locales.map { it.locale }, paths.generatedSourcesDir)
    generateEmojiGroupEnum(emojis.map { it.group }.distinct(), paths.generatedSourcesDir)
    generateEmojiSubGroupEnum(emojis.map { it.subgroup to it.group }.distinct(), paths.generatedSourcesDir)
    val publicEmojis = jsonMapper.readTree(paths.publicEmojisFile) as ArrayNode
    generatePreComputedConstants(publicEmojis, paths.generatedSourcesDir)
    generateEmojisInterfaces(publicEmojis, paths.generatedSourcesDir)

    println(WriteStats.summary())
}

private fun fetchPlatformAliases(paths: GeneratorPaths, http: Http): PlatformAliases {
    val discord = try {
        fetchDiscordAliases().also { paths.discordDefinitionFile.writeTextIfChanged(jsonMapper.writeValueAsString(it)) }
    } catch (e: Exception) {
        println("Warning: Could not fetch Discord emoji shortcuts (${e.message}), falling back to cached file")
        jsonMapper.readValue<Map<String, List<String>>>(paths.discordDefinitionFile)
    }

    val github = fetchGitHubAliases(http)
    paths.githubDefinitionFile.writeTextIfChanged(jsonMapper.writeValueAsString(github))

    return PlatformAliases(discord = discord, github = github, slack = fetchSlackAliases())
}

/** Files that contain all languages; only written with `--all`. */
private fun writeLanguageFiles(
    paths: GeneratorPaths,
    emojis: List<GradleEmoji>,
    translations: Translations,
    descriptionsByLocale: Map<String, Map<String, String>>,
) {
    translations.locales.forEach { locale ->
        // Resources of the jemoji-languages module
        val descriptions = descriptionsByLocale.getValue(locale.locale)
        paths.languageDescriptionFile(locale.locale).writeTextIfChanged(jsonMapper.writeValueAsString(descriptions))
        paths.languageKeywordFile(locale.locale).writeTextIfChanged(jsonMapper.writeValueAsString(locale.keywords))

        // Public files list every emoji, with null if the locale has no entry
        writePublicJson(paths, "description/${locale.locale}", descriptions.withNullForMissing(emojis))
        writePublicJson(paths, "keywords/${locale.locale}", locale.keywords.withNullForMissing(emojis))
    }

    writePublicJson(paths, "emojis-full", translations.withAllLanguages(emojis, descriptionsByLocale))
}

private fun <V> Map<String, V>.withNullForMissing(emojis: List<GradleEmoji>): Map<String, V?> =
    LinkedHashMap<String, V?>(this).apply { emojis.forEach { putIfAbsent(it.emoji, null) } }
