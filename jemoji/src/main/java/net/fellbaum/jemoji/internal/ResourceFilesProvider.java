package net.fellbaum.jemoji.internal;

import org.jspecify.annotations.Nullable;

import java.io.InputStream;

public interface ResourceFilesProvider {

    /**
     * Opens a resource file of the implementing module.
     * <p>
     * The resource is loaded relative to the implementing class, so that the files of the language module
     * are accessible under the module system. Parsing is done by the core module.
     *
     * @param filePathName The absolute resource path, e.g. {@code /emoji_sources/keyword/de.json}.
     * @return The resource stream, or {@code null} if the resource does not exist.
     */
    default @Nullable InputStream openLanguageFile(final String filePathName) {
        return this.getClass().getResourceAsStream(filePathName);
    }

}
