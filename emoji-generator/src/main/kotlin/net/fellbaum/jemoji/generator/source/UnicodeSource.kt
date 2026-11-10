package net.fellbaum.jemoji.generator.source

import com.jcabi.github.Coordinates
import com.jcabi.github.RtGitHub
import net.fellbaum.jemoji.Fitzpatrick
import net.fellbaum.jemoji.HairStyle
import net.fellbaum.jemoji.generator.model.GradleEmoji
import net.fellbaum.jemoji.generator.model.PlatformAliases
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Emoji list and variation sequences from the latest data in the `unicode-org/unicodetools` repository.
 */
object UnicodeSource {
    private val github = RtGitHub()
    private val coordinates = Coordinates.Simple("unicode-org", "unicodetools")

    /** Contents of the latest `emoji-test.txt`. */
    fun fetchEmojiTestFile(): String {
        val pathPrefix = "unicodetools/data/emoji/"
        val latestVersion = listVersionDirectories(pathPrefix)
            .filter { it.matches(Regex("[0-9]+\\.[0-9+]")) }
            .map { it.toDoubleOrNull() }
            .sortedBy { it }
            .toSet().last()

        return readFile("$pathPrefix$latestVersion/emoji-test.txt")
    }

    /** Contents of the latest `emoji-variation-sequences.txt`. */
    fun fetchVariationSequencesFile(): String {
        val pathPrefix = "unicodetools/data/ucd/"
        val latestVersion = listVersionDirectories(pathPrefix)
            .filter { it.matches(Regex("^[0-9]+(\\.[0-9+])+$")) }
            .sortedWith(
                compareBy(
                    { it.split(".")[0].toInt() },
                    { it.split(".")[1].toInt() },
                    { it.split(".")[2].toInt() }
                )
            )
            .toSet().last()

        return readFile("$pathPrefix$latestVersion/emoji/emoji-variation-sequences.txt")
    }

    private fun listVersionDirectories(pathPrefix: String): Sequence<String> =
        github.repos().get(coordinates)
            .contents()
            .iterate(pathPrefix, "main")
            .asSequence()
            .map { it.path().replace(pathPrefix, "") }

    private fun readFile(path: String): String =
        github.repos().get(coordinates).contents().get(path).raw().readBytes().decodeToString()
}

/**
 * Returns all base emojis that have a variation sequence.
 *
 * ```
 * 0023 FE0E  ; text style;  # (1.1) NUMBER SIGN
 * ```
 */
fun parseVariationSequences(variationSequencesFile: String): Set<String> =
    variationSequencesFile.lines()
        .asSequence()
        .filter { !it.startsWith("#") && it.isNotEmpty() }
        .map { it.split(";")[0].split(" ")[0].trim() }
        .distinct()
        .map { String(Character.toChars(it.toInt(16))) }
        .toSet()

/**
 * Parses `emoji-test.txt`, which looks like:
 *
 * ```
 * # group: Smileys & Emotion
 *
 * # subgroup: face-smiling
 * 1F600     ; fully-qualified     # 😀 E0.6 grinning face
 *
 * # subgroup: face-affection
 * 1F970     ; fully-qualified     # 🥰 E11.0 smiling face with hearts
 * ```
 *
 * Keywords are left empty; they are filled in later from CLDR.
 */
fun parseEmojiTestFile(
    emojiTestFile: String,
    emojisWithVariations: Set<String>,
    aliases: PlatformAliases,
): List<GradleEmoji> =
    // The first block is the file header
    emojiTestFile.split("# group: ").drop(1).flatMap { group ->
        val subGroups = group.split("# subgroup: ")
        // The first line is the group name, the rest of that block is empty
        val groupName = subGroups[0].lines()[0]

        subGroups.drop(1).flatMap { subGroup ->
            val subGroupLines = subGroup.lines()
            val subGroupName = subGroupLines[0]

            subGroupLines.asSequence().drop(1)
                .filter { !it.startsWith("#") && it.isNotBlank() }
                .map { parseEmojiLine(it, groupName, subGroupName, emojisWithVariations, aliases) }
                .toList()
        }
    }

/** `1F44D     ; fully-qualified     # 👍 E0.6 thumbs up` */
private fun parseEmojiLine(
    line: String,
    groupName: String,
    subGroupName: String,
    emojisWithVariations: Set<String>,
    aliases: PlatformAliases,
): GradleEmoji {
    val (codepointsPart, informationPart) = line.split(";")
    val emoji = hexCodepointsToString(codepointsPart.trim(), " ")

    // `fully-qualified     # 👍 E0.6 thumbs up`, limit 2 because of the `# keycap` emoji description
    val information = informationPart.split("#", limit = 2)
    val qualification = information[0].trim()

    // `👍 E0.6 thumbs up`
    val otherInformation = information[1].trim().split(" ", limit = 3)
    val version = otherInformation[1].removePrefix("E").toDouble()
    val description = otherInformation[2].trim()

    return GradleEmoji(
        emoji = emoji,
        // Each UTF-16 char padded to 4 digits: 👍
        unicode = emoji.asSequence().joinToString("") { "\\u%04X".format(it.code) },
        htmlDec = emoji.codePoints().toArray().joinToString("") { "&#$it;" },
        htmlHex = emoji.codePoints().toArray().joinToString("") { "&#x${Integer.toHexString(it).uppercase()};" },
        urlEncoded = URLEncoder.encode(emoji, StandardCharsets.UTF_8),
        discordAliases = aliases.discord[emoji].orEmpty().toSet(),
        githubAliases = aliases.github[emoji].orEmpty().toSet(),
        slackAliases = aliases.slack[emoji].orEmpty().toSet(),
        hasFitzpatrick = Fitzpatrick.isFitzpatrickEmoji(emoji),
        hasHairStyle = HairStyle.isHairStyleEmoji(emoji),
        version = version,
        qualification = qualification,
        description = description,
        group = groupName,
        subgroup = subGroupName,
        hasVariationSelectors = emoji in emojisWithVariations,
        keywords = emptySet<String>(),
    )
}
