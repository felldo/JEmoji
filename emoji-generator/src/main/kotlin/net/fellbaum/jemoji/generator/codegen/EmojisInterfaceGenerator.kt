package net.fellbaum.jemoji.generator.codegen

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.TypeSpec
import net.fellbaum.jemoji.Emoji
import net.fellbaum.jemoji.EmojiManager
import tools.jackson.databind.node.ArrayNode
import java.io.File
import javax.lang.model.element.Modifier

/** Keeps the generated interfaces below the JVM's constant pool limit. */
private const val EMOJIS_PER_INTERFACE = 900

/**
 * The `Emojis` interface with a constant per emoji. The constants are spread over one interface per subgroup
 * (`generated.EmojiFaceSmiling`, ...), which `Emojis` extends. Subgroups with more than [EMOJIS_PER_INTERFACE]
 * emojis are split into `...A`, `...B`, ...
 */
fun generateEmojisInterfaces(emojis: ArrayNode, outputDir: File) {
    TypeSpec.interfaceBuilder("Emojis").apply {
        addModifiers(Modifier.PUBLIC)

        for ((subgroup, subgroupEmojis) in emojis.groupBy { it["subgroup"].asString() }) {
            val constants = subgroupEmojis.map { emoji ->
                emojiConstant(
                    emojiDescriptionToConstantName(emoji["description"].asString(), emoji["qualification"].asString()),
                    emoji["emoji"].asString()
                )
            }.withUniqueNames()

            val interfaceName = emojiDescriptionToFileName(subgroup)
            val chunks = constants.chunked(EMOJIS_PER_INTERFACE)
            chunks.forEachIndexed { index, chunk ->
                val chunkInterfaceName = if (chunks.size > 1) interfaceName + ('A' + index) else interfaceName
                addSuperinterface(ClassName.get(JEMOJI_GENERATED_PACKAGE, chunkInterfaceName))
                generateSubgroupInterface(chunkInterfaceName, chunk, outputDir)
            }
        }
    }.writeJavaFile(outputDir)
}

/** `public static final Emoji NAME = EmojiManager.getEmoji("…").orElseThrow();` */
private fun emojiConstant(name: String, emoji: String): FieldSpec =
    FieldSpec.builder(ClassName.get(Emoji::class.java), name)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addJavadoc(emoji)
        .initializer(CodeBlock.of($$"$T.getEmoji($S).orElseThrow()", EmojiManager::class.java, emoji))
        .build()

/** Constants that share a name get an index suffix: `NAME_0`, `NAME_1`, ... */
private fun List<FieldSpec>.withUniqueNames(): List<FieldSpec> =
    groupBy { it.name() }.values.flatMap { sameName ->
        if (sameName.size == 1) {
            sameName
        } else {
            sameName.mapIndexed { index, field ->
                FieldSpec.builder(field.type(), field.name() + "_$index")
                    .initializer(field.initializer())
                    .addModifiers(*field.modifiers().toTypedArray())
                    .addJavadoc(field.javadoc())
                    .build()
            }
        }
    }

private fun generateSubgroupInterface(name: String, constants: List<FieldSpec>, outputDir: File) {
    TypeSpec.interfaceBuilder(name).apply {
        addModifiers(Modifier.PUBLIC)
        suppressUnusedWarnings()
        addFields(constants)
    }.writeJavaFile(outputDir, JEMOJI_GENERATED_PACKAGE)
}
