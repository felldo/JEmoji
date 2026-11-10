package net.fellbaum.jemoji.generator

import tools.jackson.databind.SerializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

/** Default mapper: keeps map entries in insertion order. */
val jsonMapper = jacksonObjectMapper()

/** Mapper for published files: map entries are sorted by key so diffs stay stable. */
val sortedJsonMapper = jacksonMapperBuilder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build()

/** Writes `public/<name>.json` (pretty printed) and `public/<name>.min.json` (compact). */
fun writePublicJson(paths: GeneratorPaths, name: String, content: Any) {
    paths.publicDir.resolve("$name.json")
        .writeTextCreatingParents(sortedJsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content))
    paths.publicDir.resolve("$name.min.json")
        .writeTextCreatingParents(sortedJsonMapper.writeValueAsString(content))
}

fun File.writeTextCreatingParents(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
