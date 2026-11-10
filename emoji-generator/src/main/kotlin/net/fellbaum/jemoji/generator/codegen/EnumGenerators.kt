package net.fellbaum.jemoji.generator.codegen

import com.fasterxml.jackson.annotation.JsonCreator
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import java.io.File
import java.util.Arrays
import java.util.EnumSet
import java.util.stream.Collectors
import javax.lang.model.element.Modifier

private val STRING: TypeName = ClassName.get(String::class.java)

/** `EmojiLanguage` with one constant per CLDR locale. */
fun generateEmojiLanguageEnum(locales: List<String>, outputDir: File) {
    TypeSpec.enumBuilder("EmojiLanguage").apply {
        addModifiers(Modifier.PUBLIC)
        locales.forEach {
            addEnumConstant(emojiGroupToEnumName(it), TypeSpec.anonymousClassBuilder($$"$S", it).addJavadoc(it).build())
        }
        addField(String::class.java, "value", Modifier.PRIVATE, Modifier.FINAL)
        addMethod(constructor("value" to STRING))
        addMethod(getter("getValue", String::class.java, "value", "Gets the value.\n\n@return The value."))
    }.writeJavaFile(outputDir)
}

/** `EmojiGroup`, e.g. `SMILEYS_AND_EMOTION("Smileys & Emotion")`. */
fun generateEmojiGroupEnum(groups: List<String>, outputDir: File) {
    val emojiGroup = jemojiType("EmojiGroup")
    val emojiSubGroup = jemojiType("EmojiSubGroup")

    TypeSpec.enumBuilder(emojiGroup.simpleName()).apply {
        addJavadoc(
            """
            Represents a group of emojis categorized by their thematic content, such as
            Activities, Animals &#38; Nature, Flags, etc.
            """.trimIndent()
        )
        addModifiers(Modifier.PUBLIC)
        suppressUnusedWarnings()
        groups.forEach {
            addEnumConstant(emojiGroupToEnumName(it), TypeSpec.anonymousClassBuilder($$"$S", it).build())
        }
        addField(allValuesField(emojiGroup, "EMOJI_GROUPS"))
        addField(String::class.java, "name", Modifier.PRIVATE, Modifier.FINAL)

        addMethod(constructor("name" to STRING))
        addMethod(
            getter(
                "getName", String::class.java, "name",
                "Gets the name of the emoji group.\n\n@return The name of the emoji group."
            )
        )
        addMethod(
            getter(
                "getGroups", parameterizedType(List::class.java, emojiGroup), "EMOJI_GROUPS",
                "Gets all emoji groups.\n\n@return All emoji groups.",
                Modifier.PUBLIC, Modifier.STATIC
            )
        )
        addMethod(fromStringMethod(emojiGroup, "EMOJI_GROUPS", "emojiGroup", "emoji group"))
        addMethod(
            MethodSpec.methodBuilder("getSubGroups")
                .addJavadoc("Gets all emoji subgroups related to this group.\n\n@return All emoji subgroups.")
                .addModifiers(Modifier.PUBLIC)
                .returns(parameterizedType(EnumSet::class.java, emojiSubGroup))
                .addStatement(
                    $$"return $T.copyOf(EmojiSubGroup.getSubGroups().stream().filter(subgroup -> subgroup.getGroup() == this).collect($T.toList()))",
                    EnumSet::class.java, Collectors::class.java
                )
                .build()
        )
    }.writeJavaFile(outputDir)
}

/** `EmojiSubGroup`, e.g. `FACE_SMILING("face-smiling", EmojiGroup.SMILEYS_AND_EMOTION)`. */
fun generateEmojiSubGroupEnum(subGroupsWithGroup: List<Pair<String, String>>, outputDir: File) {
    val emojiGroup = jemojiType("EmojiGroup")
    val emojiSubGroup = jemojiType("EmojiSubGroup")

    TypeSpec.enumBuilder(emojiSubGroup.simpleName()).apply {
        addModifiers(Modifier.PUBLIC)
        suppressUnusedWarnings()
        addJavadoc(
            """
            This enum represents various subgroups of emojis. Each subgroup is associated with
            a specific {@link EmojiGroup}, categorizing the emojis into their respective
            thematic groups. These subgroups allow for better organization and retrieval of emojis.
            """.trimIndent()
        )
        subGroupsWithGroup.forEach { (subGroup, group) ->
            addEnumConstant(
                emojiGroupToEnumName(subGroup),
                TypeSpec.anonymousClassBuilder($$"$S, EmojiGroup.$L", subGroup, emojiGroupToEnumName(group)).build()
            )
        }

        addField(allValuesField(emojiSubGroup, "EMOJI_SUBGROUPS"))
        addField(String::class.java, "name", Modifier.PRIVATE, Modifier.FINAL)
        addField(emojiGroup, "emojiGroup", Modifier.PRIVATE, Modifier.FINAL)

        addMethod(constructor("name" to STRING, "emojiGroup" to emojiGroup))
        addMethod(
            getter(
                "getName", String::class.java, "name",
                "Gets the name of the emoji sub group.\n\n@return The name of the emoji subgroup."
            )
        )
        addMethod(
            getter(
                "getSubGroups", parameterizedType(List::class.java, emojiSubGroup), "EMOJI_SUBGROUPS",
                "Gets all emoji subgroups.\n\n@return All emoji subgroups.",
                Modifier.PUBLIC, Modifier.STATIC
            )
        )
        addMethod(fromStringMethod(emojiSubGroup, "EMOJI_SUBGROUPS", "emojiSubGroup", "emoji subgroup"))
        addMethod(
            getter(
                "getGroup", emojiGroup, "emojiGroup",
                "Gets the parent group this sub group belongs to.\n\n@return The parent group."
            )
        )
    }.writeJavaFile(outputDir)
}

/** Constructor that assigns each parameter to the field of the same name. */
private fun constructor(vararg parameters: Pair<String, TypeName>): MethodSpec =
    MethodSpec.constructorBuilder().apply {
        parameters.forEach { (name, type) -> addParameter(type, name, Modifier.FINAL) }
        parameters.forEach { (name, _) -> addStatement($$"this.$N = $N", name, name) }
    }.build()

/** `private static final List<Enum> FIELD = Arrays.asList(values());` */
private fun allValuesField(enumType: ClassName, fieldName: String): FieldSpec =
    FieldSpec.builder(
        parameterizedType(List::class.java, enumType),
        fieldName,
        Modifier.PRIVATE,
        Modifier.STATIC,
        Modifier.FINAL
    ).initializer(CodeBlock.of($$"$T.asList($L)", Arrays::class.java, "values()")).build()

/** `@JsonCreator public static Enum fromString(String name)`, looking up the constant by its name field. */
private fun fromStringMethod(
    enumType: ClassName,
    valuesField: String,
    loopVariable: String,
    readableName: String,
): MethodSpec {
    val simpleName = enumType.simpleName()
    return MethodSpec.methodBuilder("fromString")
        .addJavadoc(
            "Gets the $readableName for the given name.\n\n" +
                    "@param name The name of the $readableName.\n" +
                    "@return The $readableName."
        )
        .addParameter(String::class.java, "name", Modifier.FINAL)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(enumType)
        .addCode(
            CodeBlock.of(
                """
                for (final $simpleName $loopVariable : $valuesField) {
                    if ($loopVariable.getName().equals(name)) {
                        return $loopVariable;
                     }
                }
                throw new IllegalArgumentException("No $simpleName found for name: " + name);
                """.trimIndent()
            )
        )
        .addAnnotation(JsonCreator::class.java)
        .build()
}
