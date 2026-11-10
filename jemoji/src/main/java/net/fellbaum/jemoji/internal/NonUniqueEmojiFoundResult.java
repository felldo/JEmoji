package net.fellbaum.jemoji.internal;

import net.fellbaum.jemoji.Emoji;

import java.util.List;

/**
 * @param emojis   The emojis of the found alias.
 * @param endIndex The char index (exclusive) at which the alias ends.
 */
public record NonUniqueEmojiFoundResult(List<Emoji> emojis, int endIndex) {
}
