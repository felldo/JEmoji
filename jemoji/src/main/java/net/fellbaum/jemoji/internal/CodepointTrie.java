package net.fellbaum.jemoji.internal;

import net.fellbaum.jemoji.Emoji;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A codepoint trie, used to find the longest emoji or alias starting at a given position in a text.
 * <p>
 * Scanning a text position costs one binary search per matched codepoint and stops as soon
 * as no entry shares the current prefix, instead of testing every possible entry.
 */
public final class CodepointTrie {

    private static final int[] NO_CODEPOINTS = new int[0];
    private static final CodepointTrie[] NO_NODES = new CodepointTrie[0];

    /**
     * The codepoints of the child nodes, sorted ascending so they can be binary searched.
     */
    private int[] childCodepoints = NO_CODEPOINTS;
    /**
     * The child nodes, positionally aligned with {@link #childCodepoints}.
     */
    private CodepointTrie[] childNodes = NO_NODES;
    /**
     * The emojis of the alias ending at this node, {@code null} if no alias ends here.
     */
    @Nullable
    private List<Emoji> emojis;

    private CodepointTrie() {
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
     * Builds a bitset over the codepoints of the direct children, i.e. all codepoints an entry can start with
     * when called on the root node.
     *
     * @return The bitset, indexed by {@code codepoint >>> 6}.
     */
    public long[] childCodepointBits() {
        int maxCodepoint = 0;
        for (final int codepoint : childCodepoints) maxCodepoint = Math.max(maxCodepoint, codepoint);
        final long[] bits = new long[(maxCodepoint >>> 6) + 1];
        for (final int codepoint : childCodepoints) bits[codepoint >>> 6] |= 1L << codepoint;
        return bits;
    }

    /**
     * Gets the child node continuing the alias with the given codepoint.
     *
     * @param codepoint The codepoint to follow.
     * @return The child node, or {@code null} if no alias continues with this codepoint.
     */
    @Nullable
    public CodepointTrie child(final int codepoint) {
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
    public static CodepointTrie build(final Map<CodepointSequence, List<Emoji>> aliasToEmojis) {
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

        private CodepointTrie freeze() {
            final CodepointTrie node = new CodepointTrie();
            node.emojis = emojis;

            final int childCount = children.size();
            if (childCount != 0) {
                final int[] codepoints = new int[childCount];
                int index = 0;
                for (final Integer codepoint : children.keySet()) {
                    codepoints[index++] = codepoint;
                }
                Arrays.sort(codepoints);

                final CodepointTrie[] nodes = new CodepointTrie[childCount];
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
