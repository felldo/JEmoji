package net.fellbaum.jemoji.generator.codegen

import com.palantir.javapoet.CodeBlock
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import java.io.File
import javax.lang.model.element.Modifier

private val CONSTANT_MODIFIERS = arrayOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
private val ALIAS_KEYS = listOf("discordAliases", "githubAliases", "slackAliases")

/**
 * `internal.PreComputedConstants`: lengths and starting characters that let EmojiManager skip
 * text positions that can't be the start of an emoji.
 */
fun generatePreComputedConstants(emojis: ArrayNode, outputDir: File) {
    val urlEncoded = emojis.values().map { it["urlEncoded"].asString() }
    val htmlDec = emojis.values().map { it["htmlDec"].asString() }
    val aliases = emojis.values().flatMap { it.aliases() }.distinct()

    TypeSpec.classBuilder("PreComputedConstants").apply {
        addModifiers(Modifier.FINAL, Modifier.PUBLIC)
        suppressUnusedWarnings()

        addField(intConstant("MAXIMUM_EMOJI_URL_ENCODED_LENGTH", urlEncoded.distinct().maxOf { it.codePointLength() }))
        addField(intConstant("MINIMUM_EMOJI_URL_ENCODED_LENGTH", urlEncoded.distinct().minOf { it.codePointLength() }))
        addField(intConstant("ALIAS_EMOJI_MAX_LENGTH", aliases.maxOf { it.codePointLength() }))
        addField(
            intConstant(
                "MAX_HTML_DECIMAL_SINGLE_EMOJIS_CONCATENATED_LENGTH",
                htmlDec.maxOf { html -> html.count { it == ';' } }
            )
        )
        addField(intConstant("MIN_HTML_DECIMAL_CODEPOINT_LENGTH", htmlDec.minOf { it.codePointLength() }))

        addField(
            integerSetConstant(
                "POSSIBLE_EMOJI_ALIAS_STARTER_CODEPOINTS",
                aliases.map { it.codePointAt(0) }.distinct().sorted()
            )
        )
        addField(
            integerSetConstant(
                "POSSIBLE_EMOJI_URL_ENCODED_STARTER_CODEPOINTS",
                urlEncoded.map { it.codePointAt(0) }.distinct().sorted()
            )
        )
        addField(
            stringSetConstant(
                "ALLOWED_EMOJI_URL_ENCODED_SEQUENCES",
                urlEncoded.flatMap { it.split("%") }.distinct().filter { it.isNotEmpty() }.sorted()
            )
        )
    }.writeJavaFile(outputDir, JEMOJI_INTERNAL_PACKAGE)
}

private fun JsonNode.aliases(): List<String> =
    ALIAS_KEYS.flatMap { key -> this[key].values().map { it.asString() } }

private fun String.codePointLength() = codePointCount(0, length)

private fun intConstant(name: String, value: Int): FieldSpec =
    FieldSpec.builder(TypeName.INT, name, *CONSTANT_MODIFIERS).initializer($$"$L", value.toString()).build()

private fun integerSetConstant(name: String, values: List<Int>): FieldSpec =
    FieldSpec.builder(parameterizedType(Set::class.java, Int::class.javaObjectType), name, *CONSTANT_MODIFIERS)
        .initializer(setOfInitializer($$"$L", values))
        .build()

private fun stringSetConstant(name: String, values: List<String>): FieldSpec =
    FieldSpec.builder(parameterizedType(Set::class.java, String::class.java), name, *CONSTANT_MODIFIERS)
        .initializer(setOfInitializer($$"$S", values))
        .build()

/** `Set.of(v1, v2, ...)` */
private fun setOfInitializer(placeholder: String, values: List<Any>): CodeBlock =
    CodeBlock.of($$"$T.of(" + values.joinToString(", ") { placeholder } + ")", Set::class.java, *values.toTypedArray())
