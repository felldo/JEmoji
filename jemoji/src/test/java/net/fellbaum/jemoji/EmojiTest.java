package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmojiTest {

    @Test
    public void testUnicode() {
        assertEquals("\\uD83D\\uDC4D", Emojis.THUMBS_UP.getUnicodeText());
    }

    @Test
    public void testHtmlDec() {
        assertEquals("&#128104;&#8205;&#128105;&#8205;&#128103;&#8205;&#128102;", Emojis.FAMILY_MAN_WOMAN_GIRL_BOY.getHtmlDecimalCode());
    }

    @Test
    public void testHtmlHex() {
        assertEquals("&#x1F468;&#x200D;&#x1F469;&#x200D;&#x1F467;&#x200D;&#x1F466;", Emojis.FAMILY_MAN_WOMAN_GIRL_BOY.getHtmlHexadecimalCode());
    }

    @Test
    public void loadingKeywordsWithLanguageFails() {
        assertThrowsExactly(IllegalStateException.class, () -> Emojis.THUMBS_UP.getKeywords(EmojiLanguage.DE));
    }

    // ===== Encoding properties =====

    @Test
    public void testUrlEncoded() {
        assertEquals("%F0%9F%91%8D", Emojis.THUMBS_UP.getURLEncoded());
    }

    @Test
    public void testHtmlDecimalForThumbsUp() {
        assertEquals("&#128077;", Emojis.THUMBS_UP.getHtmlDecimalCode());
    }

    // ===== Alias properties =====

    @Test
    public void testGithubAliasesNonEmpty() {
        assertFalse(Emojis.THUMBS_UP.getGithubAliases().isEmpty());
    }

    @Test
    public void testSlackAliasesNonEmpty() {
        assertFalse(Emojis.THUMBS_UP.getSlackAliases().isEmpty());
    }

    @Test
    public void testAllAliasesContainsAllPlatformAliases() {
        List<String> allAliases = Emojis.THUMBS_UP.getAllAliases();
        Emojis.THUMBS_UP.getDiscordAliases().forEach(a -> assertTrue(allAliases.contains(a), "Missing Discord alias: " + a));
        Emojis.THUMBS_UP.getGithubAliases().forEach(a -> assertTrue(allAliases.contains(a), "Missing GitHub alias: " + a));
        Emojis.THUMBS_UP.getSlackAliases().forEach(a -> assertTrue(allAliases.contains(a), "Missing Slack alias: " + a));
    }

    @Test
    public void testAllAliasesNoDuplicates() {
        List<String> allAliases = Emojis.THUMBS_UP.getAllAliases();
        assertEquals(allAliases.size(), new HashSet<>(allAliases).size());
    }

    // ===== Modifier flags =====

    @Test
    public void testHasFitzpatrickComponent() {
        assertTrue(EmojiManager.getEmoji("👋🏻").orElseThrow(IllegalStateException::new).hasFitzpatrickComponent());
        assertFalse(Emojis.THUMBS_UP.hasFitzpatrickComponent());
    }

    @Test
    public void testHasHairStyleComponent() {
        // Man: Red Hair (👨‍🦰)
        assertTrue(EmojiManager.getEmoji("\uD83D\uDC68\u200D\uD83E\uDDB0").orElseThrow(IllegalStateException::new).hasHairStyleComponent());
        assertFalse(Emojis.THUMBS_UP.hasHairStyleComponent());
    }

    // ===== Variation selector flag =====

    @Test
    public void testHasVariationSelectors() {
        assertTrue(Emojis.LIBRA.hasVariationSelectors());
        // Complex ZWJ sequences have no variation selectors
        assertFalse(Emojis.FAMILY_MAN_WOMAN_GIRL_BOY.hasVariationSelectors());
    }

    // ===== Metadata properties =====

    @Test
    public void testDescription() {
        assertNotNull(Emojis.THUMBS_UP.getDescription());
        assertFalse(Emojis.THUMBS_UP.getDescription().isEmpty());
    }

    @Test
    public void testKeywordsNonNull() {
        // Keywords in the base module are stored as an empty list;
        // they are only available via the language module (getKeywords(EmojiLanguage))
        assertNotNull(Emojis.THUMBS_UP.getKeywords());
    }

    @Test
    public void testVersionPositive() {
        assertTrue(Emojis.THUMBS_UP.getVersion() > 0);
    }

    @Test
    public void testQualificationNonNull() {
        assertNotNull(Emojis.THUMBS_UP.getQualification());
        assertEquals(Qualification.FULLY_QUALIFIED, Emojis.THUMBS_UP.getQualification());
    }

    // ===== Variations =====

    @Test
    public void testGetVariationsNonEmptyForModifiableEmoji() {
        // 👋 (waving hand) has skin tone variants
        Emoji wavingHand = EmojiManager.getEmoji("👋").orElseThrow(IllegalStateException::new);
        assertFalse(wavingHand.getVariations().isEmpty());
    }

    @Test
    public void testGetVariationsEmptyForNonModifiableEmoji() {
        // 🌍 (Earth Globe Europe-Africa) has no modifier variants
        Emoji earthGlobe = EmojiManager.getEmoji("🌍").orElseThrow(IllegalStateException::new);
        assertTrue(earthGlobe.getVariations().isEmpty());
    }

    // ===== Equality and comparison =====

    @Test
    public void testEquals() {
        Emoji e1 = Emojis.THUMBS_UP;
        Emoji e2 = EmojiManager.getEmoji("👍").orElseThrow(IllegalStateException::new);
        assertEquals(e1, e2);
        assertNotEquals(Emojis.THUMBS_DOWN, e1);
        assertNotEquals(null, e1);
    }

    @Test
    public void testHashCode() {
        Emoji e1 = Emojis.THUMBS_UP;
        Emoji e2 = EmojiManager.getEmoji("👍").orElseThrow(IllegalStateException::new);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testCompareTo() {
        assertEquals(0, Emojis.THUMBS_UP.compareTo(Emojis.THUMBS_UP));
        // FAMILY_MAN_WOMAN_GIRL_BOY has many more codepoints, so THUMBS_UP should be less
        assertTrue(Emojis.THUMBS_UP.compareTo(Emojis.FAMILY_MAN_WOMAN_GIRL_BOY) < 0);
        assertTrue(Emojis.FAMILY_MAN_WOMAN_GIRL_BOY.compareTo(Emojis.THUMBS_UP) > 0);
    }

    // ===== Deprecated method =====

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedGetUnicodeEqualsGetUnicodeText() {
        assertEquals(Emojis.THUMBS_UP.getUnicodeText(), Emojis.THUMBS_UP.getUnicode());
    }

}
