package net.fellbaum.jemoji.internal;

import net.fellbaum.jemoji.Emoji;

import java.util.List;

public record NonUniqueEmojiFoundResult(List<Emoji> emojis, int endIndex, CodepointSequence codepointSequence) {

}