module net.fellbaum.jemoji {
    requires tools.jackson.databind;
    requires static org.jspecify;

    exports net.fellbaum.jemoji;
    exports net.fellbaum.jemoji.internal to net.fellbaum.jemoji.languages;

    uses net.fellbaum.jemoji.internal.ResourceFilesProvider;
}