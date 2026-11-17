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
 *
 * Environment:
 * - `GITHUB_TOKEN` (optional): raises the GitHub API rate limit, see [GitHubToken]
 */
fun main(args: Array<String>) {
    val rootDir = args.firstOrNull { it.startsWith("--rootDir=") }
        ?.removePrefix("--rootDir=")
        ?: System.getProperty("user.dir")
    generate(GeneratorPaths(File(rootDir)), generateAll = args.contains("--all"))
}

fun generate(paths: GeneratorPaths, generateAll: Boolean = false) {
    println(GitHubToken.describe())
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

    // 2. Write the emoji data files, remembering the previous version for the change summary
    val previousEmojis = readPreviousEmojis(paths)
    paths.emojisResourceFile.writeTextIfChanged(sortedJsonMapper.writeValueAsString(emojis))
    writePublicJson(paths, "emojis", emojis)
    val changedLocales = if (generateAll) writeLanguageFiles(paths, emojis, translations, descriptionsByLocale) else null
    writeChangeSummary(paths, previousEmojis, emojis, changedLocales, translations.locales.size)

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

/**
 * Files that contain all languages; only written with `--all`.
 *
 * @return the number of locales with at least one changed file
 */
private fun writeLanguageFiles(
    paths: GeneratorPaths,
    emojis: List<GradleEmoji>,
    translations: Translations,
    descriptionsByLocale: Map<String, Map<String, String>>,
): Int {
    val changedLocales = translations.locales.count { locale ->
        // Resources of the jemoji-languages module
        val descriptions = descriptionsByLocale.getValue(locale.locale)
        val written = listOf(
            paths.languageDescriptionFile(locale.locale).writeTextIfChanged(jsonMapper.writeValueAsString(descriptions)),
            paths.languageKeywordFile(locale.locale).writeTextIfChanged(jsonMapper.writeValueAsString(locale.keywords)),
            // Public files list every emoji, with null if the locale has no entry
            writePublicJson(paths, "description/${locale.locale}", descriptions.withNullForMissing(emojis)),
            writePublicJson(paths, "keywords/${locale.locale}", locale.keywords.withNullForMissing(emojis)),
        )
        written.any { it }
    }

    writePublicJson(paths, "emojis-full", translations.withAllLanguages(emojis, descriptionsByLocale))
    return changedLocales
}

/**
 * The committed `public/emojis.json` (`git show HEAD:...`), so a retried or repeated run still compares against
 * the last commit and not against its own output. Falls back to the file on disk if git is not available.
 */
private fun readPreviousEmojis(paths: GeneratorPaths): List<GradleEmoji>? {
    val committed = try {
        val process = ProcessBuilder("git", "show", "HEAD:public/emojis.json")
            .directory(paths.rootDir)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val bytes = process.inputStream.use { it.readBytes() }
        if (process.waitFor() == 0) bytes else null
    } catch (e: Exception) {
        null
    }
    return when {
        committed != null -> jsonMapper.readValue<List<GradleEmoji>>(committed)
        paths.publicEmojisFile.isFile -> {
            println("Warning: Could not read public/emojis.json from git, comparing against the file on disk")
            jsonMapper.readValue<List<GradleEmoji>>(paths.publicEmojisFile)
        }
        else -> null
    }
}

/** Writes [GeneratorPaths.changeSummaryFile]; without a previous `public/emojis.json` there is nothing to compare. */
private fun writeChangeSummary(
    paths: GeneratorPaths,
    previousEmojis: List<GradleEmoji>?,
    emojis: List<GradleEmoji>,
    changedLocales: Int?,
    localeCount: Int,
) {
    val markdown = if (previousEmojis == null) {
        "## Emoji changes\n\nNo previous `public/emojis.json` to compare with.\n"
    } else {
        val summary = ChangeSummary(previousEmojis.map { it.toSnapshot() }, emojis.map { it.toSnapshot() })
        println("Emoji changes: ${summary.added.size} added, ${summary.removed.size} removed, " +
                "${summary.aliasMoves.size} aliases moved, ${summary.changedEmojiCount} changed")
        val translationNote = changedLocales?.let {
            "Translation files (description/, keywords/, emojis-full.json): $it of $localeCount locales changed, not listed in detail."
        }
        summary.toPullRequestBody(translationNote)
    }
    paths.changeSummaryFile.writeTextIfChanged(markdown)
    println("Change summary: ${paths.changeSummaryFile}")
}

private fun <V> Map<String, V>.withNullForMissing(emojis: List<GradleEmoji>): Map<String, V?> =
    LinkedHashMap<String, V?>(this).apply { emojis.forEach { putIfAbsent(it.emoji, null) } }
