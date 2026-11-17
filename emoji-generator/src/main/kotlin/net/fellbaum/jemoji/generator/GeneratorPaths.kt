package net.fellbaum.jemoji.generator

import java.io.File

/**
 * All files and directories the generator reads from or writes to, relative to the repository root.
 */
class GeneratorPaths(val rootDir: File) {

    /** Cached raw data from external services (Discord, GitHub, CLDR). */
    val sourceFilesDir = rootDir.resolve("emoji_source_files")
    val discordDefinitionFile = sourceFilesDir.resolve("discord-emoji-definition.json")
    val githubDefinitionFile = sourceFilesDir.resolve("github-emoji-definition.json")
    val cldrRawFilesDir = sourceFilesDir.resolve("description")

    /** Public JSON files (emojis.json, description/, keywords/). */
    val publicDir = rootDir.resolve("public")
    val publicEmojisFile = publicDir.resolve("emojis.json")

    /** Emoji data bundled into the core jemoji jar. */
    val emojisResourceFile = rootDir.resolve("jemoji/src/main/resources/jemoji/emojis.json")

    /** Per-language description/keyword files bundled into the jemoji-languages jar. */
    val languageResourcesDir = rootDir.resolve("jemoji-languages/src/main/resources/emoji_sources")
    fun languageDescriptionFile(locale: String) = languageResourcesDir.resolve("description/$locale.json")
    fun languageKeywordFile(locale: String) = languageResourcesDir.resolve("keyword/$locale.json")

    /** Generated Java sources, compiled into the core jemoji module. */
    val generatedSourcesDir = rootDir.resolve("jemoji/build/generated/jemoji")

    /**
     * Files too large for git (GitHub rejects files over 100 MiB), e.g. `emojis-full.json`. The update workflow
     * uploads them to the `generated-data` GitHub release instead of committing them.
     */
    val releaseAssetsDir = rootDir.resolve("build/release-assets")

    /** Markdown summary of the changes in `public/emojis.json`, used as body of the update pull request. */
    val changeSummaryFile = rootDir.resolve("build/emoji-changes.md")
}
