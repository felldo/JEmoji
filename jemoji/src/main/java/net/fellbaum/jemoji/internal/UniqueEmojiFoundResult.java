package net.fellbaum.jemoji.internal;

import net.fellbaum.jemoji.Emoji;

public record UniqueEmojiFoundResult(Emoji emoji, int endIndex) {

    @Override
    public String toString() {
        return "UniqueEmojiFoundResult{" +
                "emoji=" + emoji +
                ", endIndex=" + endIndex +
                '}';
    }
}
