package net.fellbaum.jemoji.generator

import tools.jackson.databind.SerializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

/** Default mapper: keeps map entries in insertion order. */
val jsonMapper = jacksonObjectMapper()

/** Mapper for published files: map entries are sorted by key so diffs stay stable. */
val sortedJsonMapper = jacksonMapperBuilder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build()

/**
 * Writes `public/<name>.json` (pretty printed) and `public/<name>.min.json` (compact).
 *
 * @return whether at least one of the two files was written
 */
fun writePublicJson(paths: GeneratorPaths, name: String, content: Any): Boolean =
    writeJsonPair(paths.publicDir, name, content)

/**
 * Writes `<dir>/<name>.json` (pretty printed) and `<dir>/<name>.min.json` (compact).
 *
 * @return whether at least one of the two files was written
 */
fun writeJsonPair(dir: File, name: String, content: Any): Boolean {
    val pretty = dir.resolve("$name.json")
        .writeTextIfChanged(sortedJsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content))
    val compact = dir.resolve("$name.min.json")
        .writeTextIfChanged(sortedJsonMapper.writeValueAsString(content))
    return pretty || compact
}
