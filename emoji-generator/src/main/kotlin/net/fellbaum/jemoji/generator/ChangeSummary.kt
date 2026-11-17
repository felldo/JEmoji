package net.fellbaum.jemoji.generator

import net.fellbaum.jemoji.generator.model.GradleEmoji

/** Platforms whose aliases are compared separately, in the order the summary lists them. */
enum class AliasPlatform(val label: String) {
    DISCORD("Discord"),
    GITHUB("GitHub"),
    SLACK("Slack"),
}

/** The fields of one emoji that [ChangeSummary] compares. Derived fields (unicode, HTML, URL) are left out. */
data class EmojiSnapshot(
    val emoji: String,
    val description: String,
    val group: String,
    val subgroup: String,
    val version: Double,
    val qualification: String,
    val hasFitzpatrick: Boolean,
    val hasHairStyle: Boolean,
    val hasVariationSelectors: Boolean,
    val aliases: Map<AliasPlatform, Set<String>>,
    val keywords: Set<String>,
)

/** Works for the entries of `emojis.json`, where description and keywords are English only. */
fun GradleEmoji.toSnapshot() = EmojiSnapshot(
    emoji = emoji,
    description = description as String,
    group = group,
    subgroup = subgroup,
    version = version,
    qualification = qualification,
    hasFitzpatrick = hasFitzpatrick,
    hasHairStyle = hasHairStyle,
    hasVariationSelectors = hasVariationSelectors,
    aliases = mapOf(
        AliasPlatform.DISCORD to discordAliases,
        AliasPlatform.GITHUB to githubAliases,
        AliasPlatform.SLACK to slackAliases,
    ),
    keywords = (keywords as Collection<*>).mapTo(LinkedHashSet()) { it as String },
)

/**
 * Compares two versions of `emojis.json` and renders the differences as Markdown for the body of the
 * "Update generated files" pull request, whose raw diff is too large for GitHub to show.
 *
 * - Aliases and keywords are compared as sets: only removed (`−`) and added (`+`) entries are listed,
 *   a changed order is ignored.
 * - An alias that is removed from one emoji and added to another on the same platform is listed once as
 *   moved, because it changes what `EmojiManager.getByAlias` returns.
 * - Skin tone and variation selector variants with the same change are collapsed into one row.
 */
class ChangeSummary(old: List<EmojiSnapshot>, new: List<EmojiSnapshot>) {

    data class AliasMove(
        val platform: AliasPlatform,
        val alias: String,
        val from: List<EmojiSnapshot>,
        val to: List<EmojiSnapshot>,
    )

    /** One row of a "changed" section: the change [text] of one or more variants of an emoji. */
    data class ChangeRow(val emojis: List<EmojiSnapshot>, val text: String)

    val added: List<EmojiSnapshot>
    val removed: List<EmojiSnapshot>
    val aliasMoves: List<AliasMove>

    /** Section title to rows, in display order. Each section only contains emojis with a change in it. */
    val changes: Map<String, List<ChangeRow>>

    /** Number of emojis (not rows) with at least one changed field, alias moves not included. */
    val changedEmojiCount: Int

    /** Number of emojis (not rows) per section of [changes]. */
    val changedEmojiCountBySection: Map<String, Int>

    init {
        val oldByEmoji = old.associateBy { it.emoji }
        val newByEmoji = new.associateBy { it.emoji }
        added = new.filter { it.emoji !in oldByEmoji }
        removed = old.filter { it.emoji !in newByEmoji }
        aliasMoves = findAliasMoves(old, new)

        val movedAliases = aliasMoves.mapTo(HashSet()) { it.platform to it.alias }
        val sections = LinkedHashMap<String, MutableList<ChangeRow>>()
        val changedEmojis = HashSet<String>()
        fun add(section: String, emoji: EmojiSnapshot, text: String) {
            sections.getOrPut(section) { ArrayList() }.add(ChangeRow(listOf(emoji), text))
            changedEmojis.add(emoji.emoji)
        }

        // Fixed section order, independent of which change comes first
        (listOf("Descriptions") + AliasPlatform.entries.map { "${it.label} aliases" } + listOf("Keywords", "Other fields"))
            .forEach { sections[it] = ArrayList() }

        for (newEmoji in new) {
            val oldEmoji = oldByEmoji[newEmoji.emoji] ?: continue
            if (oldEmoji.description != newEmoji.description) {
                add("Descriptions", newEmoji, "${oldEmoji.description} → ${newEmoji.description}")
            }
            for (platform in AliasPlatform.entries) {
                val oldAliases = oldEmoji.aliases[platform].orEmpty().filterNot { (platform to it) in movedAliases }
                val newAliases = newEmoji.aliases[platform].orEmpty().filterNot { (platform to it) in movedAliases }
                setChange(oldAliases, newAliases)?.let { add("${platform.label} aliases", newEmoji, it) }
            }
            setChange(oldEmoji.keywords, newEmoji.keywords)?.let { add("Keywords", newEmoji, it) }
            otherFieldsChange(oldEmoji, newEmoji)?.let { add("Other fields", newEmoji, it) }
        }

        changes = sections.filterValues { it.isNotEmpty() }.mapValues { (_, rows) -> collapseVariants(rows) }
        changedEmojiCountBySection = sections.filterValues { it.isNotEmpty() }.mapValues { it.value.size }
        changedEmojiCount = changedEmojis.size
    }

    fun isEmpty() = added.isEmpty() && removed.isEmpty() && aliasMoves.isEmpty() && changes.isEmpty()

    /**
     * Renders the summary. Sections are cut after [rowLimit] rows. [translationNote] is appended as is,
     * the per-language files are not compared in detail.
     */
    fun toMarkdown(rowLimit: Int = 50, translationNote: String? = null): String = buildString {
        appendLine("## Emoji changes")
        appendLine()
        // Qualified, a plain isEmpty() would resolve to the StringBuilder
        if (this@ChangeSummary.isEmpty()) {
            appendLine("No changes in `public/emojis.json`.")
        } else {
            appendLine("| | Count |")
            appendLine("|---|---:|")
            appendLine("| ➕ Added | ${added.size} |")
            appendLine("| ➖ Removed | ${removed.size} |")
            appendLine("| 🔀 Aliases moved | ${aliasMoves.size} |")
            appendLine("| ✏️ Changed | $changedEmojiCount |")

            if (added.isNotEmpty()) {
                section("➕ Added", added.size, rowLimit, "| Emoji | Description | Group / Subgroup | Version |",
                    collapseVariants(added.map { ChangeRow(listOf(it), "") }).map { row ->
                        val emoji = row.emojis.first()
                        "| ${emoji.emoji} | ${cell(emoji.description)}${variantSuffix(row)} | " +
                                "${cell(emoji.group)} / ${cell(emoji.subgroup)} | ${emoji.version} |"
                    })
            }
            if (removed.isNotEmpty()) {
                section("➖ Removed", removed.size, rowLimit, "| Emoji | Description |",
                    collapseVariants(removed.map { ChangeRow(listOf(it), "") }).map { row ->
                        "| ${row.emojis.first().emoji} | ${cell(row.emojis.first().description)}${variantSuffix(row)} |"
                    })
            }
            if (aliasMoves.isNotEmpty()) {
                section("🔀 Aliases moved", aliasMoves.size, rowLimit, "| Alias | Platform | From | To |",
                    aliasMoves.map { move ->
                        "| ${code(move.alias)} | ${move.platform.label} | " +
                                "${move.from.joinToString("<br>") { describeWithCodepoints(it) }} | " +
                                "${move.to.joinToString("<br>") { describeWithCodepoints(it) }} |"
                    })
            }
            for ((title, rows) in changes) {
                section("✏️ $title", changedEmojiCountBySection.getValue(title), rowLimit, "| Emoji | Change |",
                    rows.map { row ->
                        val emoji = row.emojis.first()
                        "| ${emoji.emoji} ${cell(emoji.description)}${variantSuffix(row)} | ${row.text} |"
                    })
            }
        }
        if (translationNote != null) {
            appendLine()
            appendLine("<sub>${translationNote}</sub>")
        }
    }

    /** Renders with fewer rows if the body would exceed GitHub's limit of 65536 characters. */
    fun toPullRequestBody(translationNote: String? = null): String {
        for (rowLimit in listOf(50, 20, 5)) {
            val markdown = toMarkdown(rowLimit, translationNote)
            if (markdown.length <= MAX_BODY_LENGTH) return markdown
        }
        return toMarkdown(5, translationNote).take(MAX_BODY_LENGTH - 100) + "\n\n_Summary truncated._\n"
    }

    private fun StringBuilder.section(title: String, count: Int, rowLimit: Int, header: String, rows: List<String>) {
        appendLine()
        val collapsed = rows.size > COLLAPSE_THRESHOLD
        if (collapsed) {
            appendLine("<details><summary><b>$title ($count)</b></summary>")
        } else {
            appendLine("### $title ($count)")
        }
        appendLine()
        appendLine(header)
        appendLine(header.replace(Regex("[^|]+"), "---"))
        rows.take(rowLimit).forEach { appendLine(it) }
        if (rows.size > rowLimit) {
            appendLine()
            appendLine("_… and ${rows.size - rowLimit} more_")
        }
        if (collapsed) {
            appendLine()
            appendLine("</details>")
        }
    }

    companion object {
        private const val MAX_BODY_LENGTH = 65_536
        private const val COLLAPSE_THRESHOLD = 10

        /** `− a − b + c` for the removed and added entries, or null if both contain the same entries. */
        fun setChange(old: Collection<String>, new: Collection<String>): String? {
            val removed = (old.toSet() - new.toSet()).sorted()
            val added = (new.toSet() - old.toSet()).sorted()
            if (removed.isEmpty() && added.isEmpty()) return null
            return (removed.map { "− ${code(it)}" } + added.map { "+ ${code(it)}" }).joinToString(" ")
        }

        private fun otherFieldsChange(old: EmojiSnapshot, new: EmojiSnapshot): String? = listOfNotNull(
            fieldChange("group", old.group, new.group),
            fieldChange("subgroup", old.subgroup, new.subgroup),
            fieldChange("version", old.version, new.version),
            fieldChange("qualification", old.qualification, new.qualification),
            fieldChange("hasFitzpatrick", old.hasFitzpatrick, new.hasFitzpatrick),
            fieldChange("hasHairStyle", old.hasHairStyle, new.hasHairStyle),
            fieldChange("hasVariationSelectors", old.hasVariationSelectors, new.hasVariationSelectors),
        ).joinToString("; ").ifEmpty { null }

        private fun fieldChange(name: String, old: Any, new: Any) =
            if (old != new) "$name: ${cell(old.toString())} → ${cell(new.toString())}" else null

        /**
         * An alias moved if it is still used on the platform, but at least one emoji lost it and at least one
         * other emoji gained it. An alias that only got added to or removed from emojis is not a move.
         */
        private fun findAliasMoves(old: List<EmojiSnapshot>, new: List<EmojiSnapshot>): List<AliasMove> =
            AliasPlatform.entries.flatMap { platform ->
                val oldOwners = aliasOwners(old, platform)
                val newOwners = aliasOwners(new, platform)
                newOwners.mapNotNull { (alias, owners) ->
                    val previousOwners = oldOwners[alias] ?: return@mapNotNull null
                    val from = previousOwners.filter { o -> owners.none { it.emoji == o.emoji } }
                    val to = owners.filter { n -> previousOwners.none { it.emoji == n.emoji } }
                    if (from.isEmpty() || to.isEmpty()) null else AliasMove(platform, alias, from, to)
                }
            }

        private fun aliasOwners(emojis: List<EmojiSnapshot>, platform: AliasPlatform): Map<String, List<EmojiSnapshot>> {
            val owners = LinkedHashMap<String, MutableList<EmojiSnapshot>>()
            emojis.forEach { emoji -> emoji.aliases[platform].orEmpty().forEach { owners.getOrPut(it) { ArrayList() }.add(emoji) } }
            return owners
        }

        /** Skin tone suffixes of aliases: Slack `:wave::skin-tone-2:` / `:dancers::skin-tone-2-3:`, Discord `:wave_tone1:`. */
        private val SKIN_TONE_ALIAS_SUFFIX = Regex("""(::skin-tone-[2-6](-[2-6])?:|_tone[1-5](_tone[1-5])?:)""")

        /**
         * Merges rows whose emojis only differ by skin tone modifiers or variation selectors and whose change
         * texts only differ by skin tone alias suffixes. The first row of each group is shown, so its order and
         * text are kept.
         */
        fun collapseVariants(rows: List<ChangeRow>): List<ChangeRow> =
            rows.groupBy { variantKey(it.emojis.first().emoji) to it.text.replace(SKIN_TONE_ALIAS_SUFFIX, ":") }
                .values
                .map { group -> ChangeRow(group.flatMap { it.emojis }, group.first().text) }

        private fun variantKey(emoji: String): String = buildString {
            emoji.codePoints()
                .filter { it != 0xFE0E && it != 0xFE0F && it !in 0x1F3FB..0x1F3FF }
                .forEach { appendCodePoint(it) }
        }

        private fun variantSuffix(row: ChangeRow): String {
            val others = row.emojis.size - 1
            return when (others) {
                0 -> ""
                1 -> " (+1 variant)"
                else -> " (+$others variants)"
            }
        }

        /** Variants with and without variation selector look the same, so moves show the codepoints too. */
        private fun describeWithCodepoints(emoji: EmojiSnapshot): String {
            val codepoints = emoji.emoji.codePoints().toArray().joinToString(" ") { "U+%04X".format(it) }
            return "${emoji.emoji} ${cell(emoji.description)} (${code(codepoints)})"
        }

        private fun cell(text: String) = text.replace("|", "\\|").replace("\n", " ")

        private fun code(text: String): String {
            val escaped = cell(text)
            return if ('`' in escaped) "`` $escaped ``" else "`$escaped`"
        }
    }
}
