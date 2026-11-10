package net.fellbaum.jemoji.generator.codegen

// These functions define public API names (e.g. Emojis.THUMBS_UP, EmojiGroup.SMILEYS_AND_EMOTION).
// Any change renames constants for library users.

// Basic allowed characters
private val descriptionReplaceRegex = Regex("[^A-Z0-9_]+")
// Fix error with emoji names like Keycap: *
private val descriptionReplaceStarRegex = Regex("\\*")
// Fix error with emoji names like Keycap: #
private val descriptionReplaceHashRegex = Regex("#")
// Fix error with emoji names like: A button (blood type)
// which results in an ending _ in the name
private val descriptionReplaceEndingUnderscoreRegex = Regex("_$")

/** `thumbs up` -> `THUMBS_UP`, non fully-qualified emojis get the qualification as suffix. */
fun emojiDescriptionToConstantName(description: String, qualification: String): String {
    val name = description
        .uppercase()
        .replace(descriptionReplaceStarRegex, "STAR")
        .replace(descriptionReplaceHashRegex, "HASH")
        .replace(descriptionReplaceRegex, "_")
        .replace(descriptionReplaceEndingUnderscoreRegex, "") +
            if (qualification != "fully-qualified") "_" + qualification.uppercase().replace(descriptionReplaceRegex, "_") else ""

    // Java identifiers can't start with a digit
    return when {
        name.startsWith("1ST") -> "FIRST" + name.substring(3)
        name.startsWith("2ND") -> "SECOND" + name.substring(3)
        name.startsWith("3RD") -> "THIRD" + name.substring(3)
        else -> name
    }
}

/** `face-smiling` -> `EmojiFaceSmiling` */
fun emojiDescriptionToFileName(description: String): String {
    return "Emoji" + description
        .split(" ").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .split("&")
        .joinToString("And") { it.replaceFirstChar(Char::uppercaseChar) }
}

/** `Smileys & Emotion` -> `SMILEYS_AND_EMOTION` */
fun emojiGroupToEnumName(group: String): String {
    return group.uppercase().replace("-", "_").replace("&", "AND").replace(" ", "_").replace("__", "_")
}
