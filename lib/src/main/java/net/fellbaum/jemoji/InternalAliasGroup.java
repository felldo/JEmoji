package net.fellbaum.jemoji;

import java.util.Collection;
import java.util.function.Function;

enum InternalAliasGroup {
    DISCORD(Emoji::getDiscordAliases),
    GITHUB(Emoji::getGithubAliases),
    SLACK(Emoji::getSlackAliases);

    private final Function<Emoji, Collection<String>> aliasCollectionSupplier;

    InternalAliasGroup(Function<Emoji, Collection<String>> aliasCollectionSupplier) {
        this.aliasCollectionSupplier = aliasCollectionSupplier;
    }

    public Function<Emoji, Collection<String>> getAliasCollectionSupplier() {
        return aliasCollectionSupplier;
    }
}
