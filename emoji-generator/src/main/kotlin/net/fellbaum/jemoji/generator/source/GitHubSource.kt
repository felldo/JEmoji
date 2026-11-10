package net.fellbaum.jemoji.generator.source

import net.fellbaum.jemoji.generator.Http
import net.fellbaum.jemoji.generator.jsonMapper
import tools.jackson.module.kotlin.readValue

private const val GITHUB_EMOJI_API_URL = "https://api.github.com/emojis"
private const val GITHUB_UNICODE_IMAGE_PREFIX = "https://github.githubassets.com/images/icons/emoji/unicode/"

/** Alias list as last checked by hand, see [fetchGitHubAliases]. */
private const val ALIASES_RESOURCE = "/github/aliases.json"

/** The emoji GitHub renders for each entry of [ALIASES_RESOURCE], same order. */
private const val EMOJIS_RESOURCE = "/github/emojis.json"

/**
 * Returns emoji -> GitHub aliases.
 *
 * The GitHub API only links an image per alias, and the image name is not reliably the emoji's codepoints.
 * So the mapping alias -> emoji is maintained by hand in two resource files: the alias list, and the
 * emojis GitHub renders for them. If the API returns a different alias list, generation fails and prints
 * the new list. To update:
 * 1. Paste the printed list into `src/main/resources/github/aliases.json`.
 * 2. Paste the printed list into a GitHub markdown editor, and copy the rendered preview
 *    (the list with the aliases replaced by emojis) into `src/main/resources/github/emojis.json`.
 */
fun fetchGitHubAliases(http: Http): Map<String, List<String>> {
    val fetchedAliases = jsonMapper.readTree(http.get(GITHUB_EMOJI_API_URL))
        .properties()
        .asSequence()
        .map { it.key to it.value.asString().uppercase() }
        .filter { (_, imageUrl) -> imageUrl.contains(GITHUB_UNICODE_IMAGE_PREFIX, ignoreCase = true) }
        .map { (name, imageUrl) ->
            colonAlias(name) to imageUrl
                .replace(GITHUB_UNICODE_IMAGE_PREFIX, "", ignoreCase = true)
                .replace(".png?v8", "", ignoreCase = true)
        }
        // {1F44D=[(:+1:, 1F44D), (:thumbsup:, 1F44D)], ...}
        .groupBy { (_, imageName) -> imageName }
        .values
        .flatMap { aliasesOfImage -> aliasesOfImage.map { (alias, _) -> alias } }

    val knownAliases = readResourceList(ALIASES_RESOURCE)
    if (fetchedAliases != knownAliases) {
        val printableList = fetchedAliases.joinToString(", ", "[", "]") { "\"$it\"" }
        error(
            "The GitHub emoji alias list changed. Update $ALIASES_RESOURCE and $EMOJIS_RESOURCE " +
                    "as described in fetchGitHubAliases():\n$printableList"
        )
    }

    val emojis = readResourceList(EMOJIS_RESOURCE)
    check(emojis.size == knownAliases.size) {
        "$EMOJIS_RESOURCE has ${emojis.size} entries but $ALIASES_RESOURCE has ${knownAliases.size}"
    }

    return buildMap<String, MutableList<String>> {
        emojis.zip(knownAliases).forEach { (emoji, alias) -> addTo(emoji, alias) }
    }
}

private fun readResourceList(path: String): List<String> {
    val stream = object {}.javaClass.getResourceAsStream(path) ?: error("Resource $path not found")
    return stream.use { jsonMapper.readValue(it) }
}
