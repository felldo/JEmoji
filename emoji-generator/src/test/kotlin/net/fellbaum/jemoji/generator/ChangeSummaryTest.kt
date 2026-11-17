package net.fellbaum.jemoji.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangeSummaryTest {

    private fun emoji(
        emoji: String,
        description: String = "desc $emoji",
        discord: Set<String> = emptySet(),
        slack: Set<String> = emptySet(),
        keywords: Set<String> = emptySet(),
        version: Double = 1.0,
    ) = EmojiSnapshot(
        emoji = emoji,
        description = description,
        group = "Group",
        subgroup = "subgroup",
        version = version,
        qualification = "fully-qualified",
        hasFitzpatrick = false,
        hasHairStyle = false,
        hasVariationSelectors = false,
        aliases = mapOf(AliasPlatform.DISCORD to discord, AliasPlatform.GITHUB to emptySet(), AliasPlatform.SLACK to slack),
        keywords = keywords,
    )

    @Test
    fun `no changes`() {
        val emojis = listOf(emoji("😀", discord = setOf(":grinning:")))
        val summary = ChangeSummary(emojis, emojis)
        assertTrue(summary.isEmpty())
        val md = summary.toMarkdown(); assertTrue(md.contains("No changes"), md)
    }

    @Test
    fun `replaced alias is shown as removed and added in one row`() {
        val summary = ChangeSummary(
            listOf(emoji("😄", discord = setOf(":smile:", ":happy:"))),
            listOf(emoji("😄", discord = setOf(":smile:", ":joyful:"))),
        )
        val rows = summary.changes.getValue("Discord aliases")
        assertEquals(1, rows.size)
        assertEquals("− `:happy:` + `:joyful:`", rows.single().text)
        assertTrue(summary.aliasMoves.isEmpty())
    }

    @Test
    fun `first alias removed and new one added at the same time`() {
        val summary = ChangeSummary(
            listOf(emoji("👍", discord = setOf(":thumbsup:", ":+1:"))),
            listOf(emoji("👍", discord = setOf(":+1:", ":thumbs_up:"))),
        )
        assertEquals("− `:thumbsup:` + `:thumbs_up:`", summary.changes.getValue("Discord aliases").single().text)
    }

    @Test
    fun `changed order is ignored`() {
        assertNull(ChangeSummary.setChange(listOf(":a:", ":b:"), listOf(":b:", ":a:")))
        val summary = ChangeSummary(
            listOf(emoji("😀", keywords = linkedSetOf("a", "b"))),
            listOf(emoji("😀", keywords = linkedSetOf("b", "a"))),
        )
        assertTrue(summary.isEmpty())
    }

    @Test
    fun `alias moving to another emoji is listed as move only`() {
        val summary = ChangeSummary(
            listOf(emoji("❤", discord = setOf(":heart:", ":red_heart:")), emoji("❤️")),
            listOf(emoji("❤", discord = setOf(":red_heart:")), emoji("❤️", discord = setOf(":heart:"))),
        )
        val move = summary.aliasMoves.single()
        assertEquals(AliasPlatform.DISCORD, move.platform)
        assertEquals(":heart:", move.alias)
        assertEquals(listOf("❤"), move.from.map { it.emoji })
        assertEquals(listOf("❤️"), move.to.map { it.emoji })
        assertTrue(summary.changes.isEmpty(), "moved alias must not appear as per-emoji change: ${summary.changes}")
        assertTrue(summary.toMarkdown().contains("`U+2764 U+FE0F`"))
    }

    @Test
    fun `alias moving to a new emoji is a move`() {
        val summary = ChangeSummary(
            listOf(emoji("🫠", slack = setOf(":melting:"))),
            listOf(emoji("🫠"), emoji("🫥", slack = setOf(":melting:"))),
        )
        assertEquals(":melting:", summary.aliasMoves.single().alias)
        assertEquals(listOf("🫥"), summary.added.map { it.emoji })
        assertTrue(summary.changes.isEmpty())
    }

    @Test
    fun `alias shared by two emojis and removed from one is not a move`() {
        val summary = ChangeSummary(
            listOf(emoji("❤", discord = setOf(":heart:")), emoji("❤️", discord = setOf(":heart:"))),
            listOf(emoji("❤"), emoji("❤️", discord = setOf(":heart:"))),
        )
        assertTrue(summary.aliasMoves.isEmpty())
        assertEquals("− `:heart:`", summary.changes.getValue("Discord aliases").single().text)
    }

    @Test
    fun `skin tone and variation selector variants are collapsed`() {
        val skinTones = listOf("🫹", "🫹🏻", "🫹🏼", "🫹🏽", "🫹🏾", "🫹🏿")
        val summary = ChangeSummary(
            listOf(emoji("❣️", keywords = setOf("heart")), emoji("❣", keywords = setOf("heart"))),
            listOf(emoji("❣️", keywords = setOf("heart", "bang")), emoji("❣", keywords = setOf("heart", "bang"))) +
                    skinTones.map { emoji(it) },
        )
        val keywordRow = summary.changes.getValue("Keywords").single()
        assertEquals(listOf("❣️", "❣"), keywordRow.emojis.map { it.emoji })
        assertEquals(2, summary.changedEmojiCountBySection.getValue("Keywords"))

        val markdown = summary.toMarkdown()
        assertTrue(markdown.contains("(+1 variant)"))
        assertTrue(markdown.contains("(+5 variants)"))
        assertTrue(markdown.contains("### ➕ Added (6)"))
    }

    @Test
    fun `skin tone alias suffixes are collapsed`() {
        val tones = listOf("🫷" to ":", "🫷🏻" to "::skin-tone-2:", "🫷🏿" to "::skin-tone-6:")
        val summary = ChangeSummary(
            tones.map { (e, _) -> emoji(e) },
            tones.map { (e, suffix) -> emoji(e, slack = setOf(":leftwards_pushing_hand$suffix")) },
        )
        val row = summary.changes.getValue("Slack aliases").single()
        assertEquals("+ `:leftwards_pushing_hand:`", row.text)
        assertEquals(3, row.emojis.size)

        val twoTones = listOf("🧑🏻‍🐰‍🧑🏼" to "2-3", "🧑🏿‍🐰‍🧑🏾" to "6-5")
        val twoToneSummary = ChangeSummary(
            twoTones.map { (e, _) -> emoji(e) },
            twoTones.map { (e, tones) -> emoji(e, slack = setOf(":dancers::skin-tone-$tones:")) },
        )
        assertEquals(2, twoToneSummary.changes.getValue("Slack aliases").single().emojis.size)
    }

    @Test
    fun `different changes of variants are not collapsed`() {
        val summary = ChangeSummary(
            listOf(emoji("❣️", keywords = setOf("heart")), emoji("❣", keywords = setOf("heart"))),
            listOf(emoji("❣️", keywords = setOf("heart", "bang")), emoji("❣", keywords = setOf("heart", "mark"))),
        )
        assertEquals(2, summary.changes.getValue("Keywords").size)
    }

    @Test
    fun `description and other fields`() {
        val summary = ChangeSummary(
            listOf(emoji("🇭🇲", description = "flag: Heard & McDonald Islands", version = 2.0)),
            listOf(emoji("🇭🇲", description = "flag: Heard Island & McDonald Islands", version = 2.1)),
        )
        assertEquals(
            "flag: Heard & McDonald Islands → flag: Heard Island & McDonald Islands",
            summary.changes.getValue("Descriptions").single().text,
        )
        assertEquals("version: 2.0 → 2.1", summary.changes.getValue("Other fields").single().text)
        assertEquals(1, summary.changedEmojiCount)
    }

    @Test
    fun `long sections are collapsed and cut`() {
        val old = (0 until 30).map { emoji("x$it") }
        val new = old.map { it.copy(keywords = setOf("new")) }
        val markdown = ChangeSummary(old, new).toMarkdown(rowLimit = 20)
        assertTrue(markdown.contains("<details><summary><b>✏️ Keywords (30)</b></summary>"))
        assertTrue(markdown.contains("_… and 10 more_"))
    }

    @Test
    fun `pull request body stays below the GitHub limit`() {
        val old = (0 until 5000).map { emoji("x$it", slack = setOf(":a$it:")) }
        val new = old.map { it.copy(description = it.description + " ".repeat(200) + "changed", keywords = setOf("k")) }
        assertTrue(ChangeSummary(old, new).toPullRequestBody("note").length <= 65_536)
    }
}
