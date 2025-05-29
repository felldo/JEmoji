package net.fellbaum.jemoji;

import java.util.Arrays;

final class InternalCodepointSequence {
    private final int[] codepoints;

    public int[] getCodepoints() {
        return codepoints;
    }

    public InternalCodepointSequence(int[] codepoints) {
        this.codepoints = codepoints;
    }

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