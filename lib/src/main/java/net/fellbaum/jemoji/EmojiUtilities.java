package net.fellbaum.jemoji;

public final class EmojiUtilities {

    private EmojiUtilities() {
    }

    /**
     * Loads all emoji descriptions into memory to avoid potential description file reads during operation.
     * This will most likely be called on startup of your application.
     */
    public static void loadAllEmojiDescriptions() {
        for (EmojiLanguage value : EmojiLanguage.values()) {
            EmojiManager.getEmojiDescriptionForLanguageAndEmoji(value, Emojis.THUMBS_UP.getEmoji());
        }
    }

    /**
     * Loads all emoji descriptions into memory to avoid potential description file reads during operation.
     * This will most likely be called on startup of your application.
     */
    public static void loadAllEmojiKeywords() {
        for (EmojiLanguage value : EmojiLanguage.values()) {
            EmojiManager.getEmojiKeywordsForLanguageAndEmoji(value, Emojis.THUMBS_UP.getEmoji());
        }
    }
}
