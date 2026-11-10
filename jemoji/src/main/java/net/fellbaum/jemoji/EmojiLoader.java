package net.fellbaum.jemoji;

import net.fellbaum.jemoji.internal.ResourceFilesProvider;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Utility class for managing and loading emojis, including descriptions and keywords.
 * The class handles reading emoji-related data from resource files.
 */
public final class EmojiLoader {

    private EmojiLoader() {
    }

    @Nullable
    private static final ResourceFilesProvider RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE;

    private static List<ResourceFilesProvider> providers() {
        final List<ResourceFilesProvider> services = new ArrayList<>();
        final ServiceLoader<ResourceFilesProvider> loader = ServiceLoader.load(ResourceFilesProvider.class);
        loader.forEach(services::add);
        return services;
    }

    private static final ObjectMapper LANGUAGE_FILE_MAPPER = new ObjectMapper();

    /**
     * Reads {@code <fileName><language>.json} from the language module and deserializes it into {@code type}.
     */
    static <T> T readFromAllLanguageResourceFiles(final String fileName, final EmojiLanguage language, final TypeReference<T> type) {
        if (RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE == null) {
            throw new IllegalStateException("Trying to access a property for language \"" + language.getValue() + "\" but the jemoji-language module is missing. To add multi language support, see here https://github.com/felldo/JEmoji?tab=readme-ov-file#-jemoji-language-module");
        }
        final String filePathName = fileName + language.getValue() + ".json";
        final InputStream is = RESOURCE_FILES_PROVIDER_LANGUAGE_MODULE.openLanguageFile(filePathName);
        if (is == null) {
            throw new IllegalStateException("Language file not found: " + filePathName);
        }
        // readValue closes the stream
        return LANGUAGE_FILE_MAPPER.readValue(is, type);
    }

    static {
        final List<ResourceFilesProvider> providers = providers();
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

    /**
     * Loads all emoji descriptions into memory to avoid potential description file reads during operation.
     * This will most likely be called once on startup of your application.
     */
    public static void loadAllEmojiDescriptions() {
        for (final EmojiLanguage value : EmojiLanguage.values()) {
            EmojiManager.getEmojiDescriptionForLanguageAndEmoji(value, Emojis.THUMBS_UP.getEmoji());
        }
    }

    /**
     * Loads all emoji keywords into memory to avoid potential keyword file reads during operation.
     * This will most likely be called once on startup of your application.
     */
    public static void loadAllEmojiKeywords() {
        for (final EmojiLanguage value : EmojiLanguage.values()) {
            EmojiManager.getEmojiKeywordsForLanguageAndEmoji(value, Emojis.THUMBS_UP.getEmoji());
        }
    }

}
