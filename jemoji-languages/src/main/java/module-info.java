module net.fellbaum.jemoji.languages {
    requires net.fellbaum.jemoji;

    provides net.fellbaum.jemoji.internal.ResourceFilesProvider
        with net.fellbaum.jemoji.languages.internal.ResourceFilesManager;
}