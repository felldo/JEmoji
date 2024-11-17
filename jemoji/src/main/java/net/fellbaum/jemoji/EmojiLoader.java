package net.fellbaum.jemoji;

import net.fellbaum.jemoji.internal.ResourceFilesProvider;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class EmojiLoader {

    private EmojiLoader() {
    }

//    static Object readFileAsObject(final String filePathName) {
//        try {
//            try (final InputStream is = EmojiManager.class.getResourceAsStream(filePathName)) {
//                if (null == is) throw new IllegalStateException("InputStream is null");
//                final ObjectInputStream ois = new ObjectInputStream(is);
//                final Object readObject = ois.readObject();
//                ois.close();
//                return readObject;
//            } catch (final ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        } catch (final IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    static final String DEFAULT_PROVIDER = "net.fellbaum.jemoji.internal.ResourceFilesManager";
    @Nullable
    static final ResourceFilesProvider RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE;

    public static List<ResourceFilesProvider> providers() {
        List<ResourceFilesProvider> services = new ArrayList<>();
        ServiceLoader<ResourceFilesProvider> loader = ServiceLoader.load(ResourceFilesProvider.class);
        loader.forEach(services::add);
        return services;
    }

    static Object readFromAllLanguageResourceFiles(String fileName, EmojiLanguage language) {
        if (RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE == null) {
            throw new IllegalStateException("Trying to access a property for language \"" + language.getValue() + "\" but the jemoji-language module is missing. To add multi language support, see here https://github.com/felldo/JEmoji?tab=readme-ov-file#-jemoji-language-module");
        }
        return RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE.readFileAsObject(fileName + language.getValue());
    }

    static {
        List<ResourceFilesProvider> providers = EmojiLoader.providers();
        switch (providers.size()) {
            case 1: {
                RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE = providers.get(0);
                break;
            }
            case 0: {
                RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE = null;
                break;
            }
            default: {
                throw new IllegalStateException("Found too many ResourceFilesProviders");
            }
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
