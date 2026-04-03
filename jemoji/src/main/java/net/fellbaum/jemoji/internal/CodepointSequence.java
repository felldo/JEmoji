package net.fellbaum.jemoji.internal;

import java.util.Arrays;

public record CodepointSequence(int[] codepoints) {

    public CodepointSequence(String text) {
        this(EmojiUtils.stringToCodePoints(text));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CodepointSequence && Arrays.equals(this.codepoints, ((CodepointSequence) obj).codepoints);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(codepoints);
    }
}