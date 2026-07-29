package net.fellbaum.jemoji.internal;

import net.fellbaum.jemoji.Emoji;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A codepoint trie over all emoji aliases, used to find the longest alias starting at a
 * given position in a text.
 * <p>
 * Scanning a text position costs one binary search per matched codepoint and stops as soon
 * as no alias shares the current prefix, instead of testing every possible alias length.
 */
public final class AliasTrie {

    private static final int[] NO_CODEPOINTS = new int[0];
    private static final AliasTrie[] NO_NODES = new AliasTrie[0];

    /**
     * The codepoints of the child nodes, sorted ascending so they can be binary searched.
     */
    private int[] childCodepoints = NO_CODEPOINTS;
    /**
     * The child nodes, positionally aligned with {@link #childCodepoints}.
     */
    private AliasTrie[] childNodes = NO_NODES;
    /**
     * The emojis of the alias ending at this node, {@code null} if no alias ends here.
     */
    @Nullable
    private List<Emoji> emojis;

    private AliasTrie() {
    }

    /**
     * Gets the emojis of the alias ending at this node.
     *
     * @return The emojis, or {@code null} if no alias ends at this node.
     */
    @Nullable
    public List<Emoji> getEmojis() {
        return emojis;
    }

    /**
     * Gets the child node continuing the alias with the given codepoint.
     *
     * @param codepoint The codepoint to follow.
     * @return The child node, or {@code null} if no alias continues with this codepoint.
     */
    @Nullable
    public AliasTrie child(final int codepoint) {
        final int[] codepoints = childCodepoints;
        int low = 0;
        int high = codepoints.length - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final int midCodepoint = codepoints[mid];
            if (midCodepoint < codepoint) {
                low = mid + 1;
            } else if (midCodepoint > codepoint) {
                high = mid - 1;
            } else {
                return childNodes[mid];
            }
        }
        return null;
    }

    /**
     * Builds a trie containing every alias of the given map.
     *
     * @param aliasToEmojis The aliases mapped to their emojis.
     * @return The root node of the trie.
     */
    public static AliasTrie build(final Map<CodepointSequence, List<Emoji>> aliasToEmojis) {
        final Builder root = new Builder();
        for (final Map.Entry<CodepointSequence, List<Emoji>> entry : aliasToEmojis.entrySet()) {
            final int[] codepoints = entry.getKey().codepoints();
            if (codepoints.length == 0) continue;

            Builder node = root;
            for (final int codepoint : codepoints) {
                node = node.children.computeIfAbsent(codepoint, ignored -> new Builder());
            }
            node.emojis = entry.getValue();
        }
        return root.freeze();
    }

    private static final class Builder {

        private final Map<Integer, Builder> children = new HashMap<>();
        @Nullable
        private List<Emoji> emojis;

        private AliasTrie freeze() {
            final AliasTrie node = new AliasTrie();
            node.emojis = emojis;

            final int childCount = children.size();
            if (childCount != 0) {
                final int[] codepoints = new int[childCount];
                int index = 0;
                for (final Integer codepoint : children.keySet()) {
                    codepoints[index++] = codepoint;
                }
                Arrays.sort(codepoints);

                final AliasTrie[] nodes = new AliasTrie[childCount];
                for (int i = 0; i < childCount; i++) {
                    nodes[i] = children.get(codepoints[i]).freeze();
                }

                node.childCodepoints = codepoints;
                node.childNodes = nodes;
            }

            return node;
        }
    }
}
