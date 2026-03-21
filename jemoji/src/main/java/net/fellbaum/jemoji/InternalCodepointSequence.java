package net.fellbaum.jemoji;

import java.util.Arrays;

record InternalCodepointSequence(int[] codepoints) {

    public InternalCodepointSequence(String text) {
        this(InternalEmojiUtils.stringToCodePoints(text));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof InternalCodepointSequence && Arrays.equals(this.codepoints, ((InternalCodepointSequence) obj).codepoints);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(codepoints);
    }
}