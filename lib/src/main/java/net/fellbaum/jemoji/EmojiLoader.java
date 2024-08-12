package net.fellbaum.jemoji;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class EmojiLoader {

    private EmojiLoader() {
    }

    static Object readFileAsObject(final String filePathName) {
        try {
            try (final InputStream is = EmojiManager.class.getResourceAsStream(filePathName)) {
                if (null == is) throw new IllegalStateException("InputStream is null");
                final ObjectInputStream ois = new ObjectInputStream(is);
                final Object readObject = ois.readObject();
                ois.close();
                return readObject;
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String readFileAsString(final String filePathName) {
        try {
            try (final InputStream is = EmojiManager.class.getResourceAsStream(filePathName)) {
                if (null == is) throw new IllegalStateException("InputStream is null");
                try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                     final BufferedReader reader = new BufferedReader(isr)) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads all emoji descriptions into memory to avoid potential description file reads during operation.
     * This will most likely be called once on startup of your application.
     */
    public static void loadAllEmojiDescriptions() {
        for (EmojiLanguage value : EmojiLanguage.values()) {
            EmojiManager.getEmojiDescriptionForLanguageAndEmoji(value, Emojis.THUMBS_UP.getEmoji());
        }
    }

    /**
     * Loads all emoji descriptions into memory to avoid potential description file reads during operation.
     * This will most likely be called once on startup of your application.
     */
    public static void loadAllEmojiKeywords() {
        for (EmojiLanguage value : EmojiLanguage.values()) {
            EmojiManager.getEmojiKeywordsForLanguageAndEmoji(value, Emojis.THUMBS_UP.getEmoji());
        }
    }

}
