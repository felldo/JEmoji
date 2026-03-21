module net.fellbaum.jemoji {
    requires tools.jackson.databind;
    requires static org.jspecify;
    requires static java.compiler;

    exports net.fellbaum.jemoji;
    exports net.fellbaum.jemoji.internal to net.fellbaum.jemoji.languages;

    uses net.fellbaum.jemoji.internal.ResourceFilesProvider;
}