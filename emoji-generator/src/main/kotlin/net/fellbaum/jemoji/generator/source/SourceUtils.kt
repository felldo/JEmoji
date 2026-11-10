package net.fellbaum.jemoji.generator.source

/** `thumbsup` -> `:thumbsup:` */
fun colonAlias(name: String) = ":$name:"

/** `1F44D-1F3FB` (with the given separator) -> 👍🏻 */
fun hexCodepointsToString(hex: String, separator: String): String =
    hex.split(separator).joinToString("") { String(Character.toChars(it.toInt(16))) }

fun MutableMap<String, MutableList<String>>.addTo(key: String, value: String) {
    getOrPut(key) { mutableListOf() }.add(value)
}
