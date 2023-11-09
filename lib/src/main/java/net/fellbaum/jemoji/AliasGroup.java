package net.fellbaum.jemoji;

import java.util.Collection;
import java.util.function.Function;

enum AliasGroup {
    DISCORD(Emoji::getDiscordAliases),
    GITHUB(Emoji::getGithubAliases),
    SLACK(Emoji::getSlackAliases);

    private final Function<Emoji, Collection<String>> aliasCollectionSupplier;

    AliasGroup(Function<Emoji, Collection<String>> aliasCollectionSupplier) {
        this.aliasCollectionSupplier = aliasCollectionSupplier;
    }

    public Function<Emoji, Collection<String>> getAliasCollectionSupplier() {
        return aliasCollectionSupplier;
    }
}
