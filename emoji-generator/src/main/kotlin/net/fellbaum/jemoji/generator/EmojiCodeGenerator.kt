package net.fellbaum.jemoji.generator

import com.fasterxml.jackson.annotation.JsonCreator
import com.palantir.javapoet.*
import net.fellbaum.jemoji.*
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.util.*
import java.util.stream.Collectors
import javax.lang.model.element.Modifier
import kotlin.math.ceil

private val jemojiPackagePath = listOf("net", "fellbaum", "jemoji")
private val jemojiBasePackagePathString = jemojiPackagePath.joinToString(".")
private val jemojiGeneratedPackagePathString = "$jemojiBasePackagePathString.generated"

// FIX DESCRIPTION ERRORS THAT CAUSE COMPILATION ERRORS WITH NAMES
// Basic allowed characters
private val descriptionReplaceRegex = Regex("[^A-Z0-9_]+")
// Fix error with emoji names like Keycap: *
private val descriptionReplaceStarRegex = Regex("\\*")
// Fix error with emoji names like Keycap: #
private val descriptionReplaceHashRegex = Regex("#")
// Fix error with emoji names like: A button (blood type)
// which results in an ending _ in the name
private val descriptionReplaceEndingUnderscoreRegex = Regex("_$")

/**
 * Startup task to generate the necessary source files for this project. Does not generate a new emojis.json.
 */
fun generateJavaSourceFiles(rootDir: String) {
    val emojisPerInterface = 900
    val emojiArrayNode: ArrayNode =
        jacksonObjectMapper().readTree(File("$rootDir/public/emojis.json")) as ArrayNode

    createStaticConstantsClassFromPreComputation(emojiArrayNode, rootDir)

    TypeSpec.interfaceBuilder("Emojis").apply {
        addModifiers(Modifier.PUBLIC)

        for (entry in emojiArrayNode.groupBy { it.get("subgroup").asString() }) {
            val emojiSubGroupInterfaceConstantVariables = mutableListOf<FieldSpec>()
            val emojiSubGroupInterfaceConstantVariablesValidNames = mutableListOf<FieldSpec>()
            entry.value.forEach {
                val constantName: String =
                    emojiDescriptionToConstantName(it.get("description").asString(), it.get("qualification").asString())

                val emojiConstantVariable = FieldSpec.builder(ClassName.get(Emoji::class.java), constantName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addJavadoc(it.get("emoji").asString())
                    .initializer(
                        CodeBlock.of(
                            $$"$T.getEmoji($S).orElseThrow()",
                            EmojiManager::class.java,
                            it.get("emoji").asString()
                        )
                    )
                    .build()
//                $$"$T.getEmoji($S).orElseThrow($T::new)"
//                java.lang.IllegalStateException::class.java
                emojiSubGroupInterfaceConstantVariables.add(emojiConstantVariable)
            }

            emojiSubGroupInterfaceConstantVariables.groupBy { it.name() }.forEach {
                if (it.value.size > 1) {
                    it.value.forEachIndexed { index, emojiFieldSpec ->
                        emojiSubGroupInterfaceConstantVariablesValidNames.add(
                            FieldSpec.builder(
                                emojiFieldSpec.type(),
                                emojiFieldSpec.name() + "_$index"
                            ).initializer(emojiFieldSpec.initializer())
                                .addModifiers(*emojiFieldSpec.modifiers().toTypedArray())
                                .addJavadoc(emojiFieldSpec.javadoc())
                                .build()
                        )
                    }
                } else {
                    emojiSubGroupInterfaceConstantVariablesValidNames.add(it.value.first())
                }
            }

            // Create multiple interfaces of the same SubGroup, if there are more than X emojis
            val emojiSubgroupFileName = emojiDescriptionToFileName(entry.key)
            if (ceil(emojiSubGroupInterfaceConstantVariablesValidNames.size / emojisPerInterface.toDouble()) > 1) {
                var startingLetter = 'A'
                emojiSubGroupInterfaceConstantVariablesValidNames.windowed(emojisPerInterface, emojisPerInterface, true)
                    .forEach {
                        val adjustedInterfaceName = emojiSubgroupFileName + (startingLetter++)
                        addSuperinterface(ClassName.get(jemojiGeneratedPackagePathString, adjustedInterfaceName))
                        createSubGroupEmojiInterface(adjustedInterfaceName, it, rootDir)
                    }
            } else {
                addSuperinterface(ClassName.get(jemojiGeneratedPackagePathString, emojiSubgroupFileName))
                createSubGroupEmojiInterface(emojiSubgroupFileName, emojiSubGroupInterfaceConstantVariablesValidNames, rootDir)
            }
        }
    }.saveGeneratedJavaSourceFile(rootDir)
}

fun generateEmojiLanguageEnum(languages: List<String>, rootDir: String) {
    TypeSpec.enumBuilder("EmojiLanguage").apply {
        addModifiers(Modifier.PUBLIC)
        languages.forEach {
            addEnumConstant(emojiGroupToEnumName(it), TypeSpec.anonymousClassBuilder($$"$S", it).addJavadoc(it).build())
        }
        addField(
            String::class.java,
            "value",
            Modifier.PRIVATE,
            Modifier.FINAL
        )
        addMethod(
            MethodSpec.constructorBuilder()
                .addParameter(String::class.java, "value", Modifier.FINAL)
                .addStatement($$"this.$N = $N", "value", "value")
                .build()
        )
        addMethod(
            MethodSpec.methodBuilder("getValue")
                .addJavadoc(
                    """
                Gets the value.

                @return The value.
            """.trimIndent()
                )
                .addModifiers(Modifier.PUBLIC)
                .returns(String::class.java)
                .addStatement($$"return $N", "value")
                .build()
        )
    }.saveGeneratedJavaSourceFile(rootDir)
}

fun generateEmojiGroupEnum(groups: List<String>, rootDir: String) {
    TypeSpec.enumBuilder("EmojiGroup").apply {
        addJavadoc(
            CodeBlock.of(
                """
            Represents a group of emojis categorized by their thematic content, such as
            Activities, Animals &#38; Nature, Flags, etc.
        """.trimIndent()
            )
        )
        addModifiers(Modifier.PUBLIC)
        addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", $$"$S", "unused").build())
        groups.forEach {
            addEnumConstant(emojiGroupToEnumName(it), TypeSpec.anonymousClassBuilder($$"$S", it).build())
        }
        addField(
            FieldSpec.builder(
                getParameterizedTypName(List::class.java, jemojiBasePackagePathString, "EmojiGroup"),
                "EMOJI_GROUPS",
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
            ).initializer(
                CodeBlock.of(
                    $$"$T.asList($L)",
                    Arrays::class.java,
                    "values()"
                )
            ).build()
        )
        addField(
            String::class.java,
            "name",
            Modifier.PRIVATE,
            Modifier.FINAL
        )

        addMethod(
            MethodSpec.constructorBuilder()
                .addParameter(String::class.java, "name", Modifier.FINAL)
                .addStatement($$"this.$N = $N", "name", "name")
                .build()
        )
        addMethod(
            MethodSpec.methodBuilder("getName")
                .addJavadoc(
                    """
                Gets the name of the emoji group.

                @return The name of the emoji group.
            """.trimIndent()
                )
                .addModifiers(Modifier.PUBLIC)
                .returns(String::class.java)
                .addStatement($$"return $N", "name")
                .build()
        )

        addMethod(
            MethodSpec.methodBuilder("getGroups")
                .addJavadoc(
                    """
                Gets all emoji groups.

                @return All emoji groups.
            """.trimIndent()
                )
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(getParameterizedTypName(List::class.java, jemojiBasePackagePathString, "EmojiGroup"))
                .addStatement($$"return $N", "EMOJI_GROUPS")
                .build()
        )

        addMethod(
            MethodSpec.methodBuilder("fromString")
                .addJavadoc(
                    """
                Gets the emoji group for the given name.

                @param name The name of the emoji group.
                @return The emoji group.
            """.trimIndent()
                )
                .addParameter(String::class.java, "name", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(jemojiBasePackagePathString, "EmojiGroup"))
                .addCode(
                    CodeBlock.of(
                        """
                        for (final EmojiGroup emojiGroup : EMOJI_GROUPS) {
                            if (emojiGroup.getName().equals(name)) {
                                return emojiGroup;
                             }
                        }
                        throw new IllegalArgumentException("No EmojiGroup found for name: " + name);
                    """.trimIndent()
                    )
                )
                .addAnnotation(JsonCreator::class.java)
                .build()
        )

        addMethod(
            MethodSpec.methodBuilder("getSubGroups")
                .addJavadoc(
                    """
                Gets all emoji subgroups related to this group.

                @return All emoji subgroups.
            """.trimIndent()
                )
                .addModifiers(Modifier.PUBLIC)
                .returns(getParameterizedTypName(EnumSet::class.java, jemojiBasePackagePathString, "EmojiSubGroup"))
                .addStatement(
                    $$"return $T.copyOf(EmojiSubGroup.getSubGroups().stream().filter(subgroup -> subgroup.getGroup() == this).collect($T.toList()))",
                    EnumSet::class.java, Collectors::class.java
                )
                .build()
        )


    }.saveGeneratedJavaSourceFile(rootDir)
}

fun generateEmojiSubGroupEnum(groups: List<Pair<String, String>>, rootDir: String) {
    TypeSpec.enumBuilder("EmojiSubGroup").apply {
        addModifiers(Modifier.PUBLIC)
        addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", $$"$S", "unused").build())
        addJavadoc(
            CodeBlock.of(
                """
             This enum represents various subgroups of emojis. Each subgroup is associated with
             a specific {@link EmojiGroup}, categorizing the emojis into their respective
             thematic groups. These subgroups allow for better organization and retrieval of emojis.
        """.trimIndent()
            )
        )
        groups.forEach {
            addEnumConstant(
                emojiGroupToEnumName(it.first),
                TypeSpec.anonymousClassBuilder($$"$S, EmojiGroup.$L", it.first, emojiGroupToEnumName(it.second)).build()
            )
        }

        addField(
            FieldSpec.builder(
                getParameterizedTypName(List::class.java, jemojiBasePackagePathString, "EmojiSubGroup"),
                "EMOJI_SUBGROUPS",
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
            ).initializer(
                CodeBlock.of(
                    $$"$T.asList($L)",
                    Arrays::class.java,
                    "values()"
                )
            ).build()
        )
        addField(
            String::class.java,
            "name",
            Modifier.PRIVATE,
            Modifier.FINAL
        )
        addField(
            ClassName.get(jemojiBasePackagePathString, "EmojiGroup"),
            "emojiGroup",
            Modifier.PRIVATE,
            Modifier.FINAL
        )
        addMethod(
            MethodSpec.constructorBuilder()
                .addParameter(String::class.java, "name", Modifier.FINAL)
                .addParameter(ClassName.get(jemojiBasePackagePathString, "EmojiGroup"), "emojiGroup", Modifier.FINAL)
                .addStatement($$"this.$N = $N", "name", "name")
                .addStatement($$"this.$N = $N", "emojiGroup", "emojiGroup")
                .build()
        )
        addMethod(
            MethodSpec.methodBuilder("getName")
                .addJavadoc(
                    """
                Gets the name of the emoji sub group.

                @return The name of the emoji subgroup.
            """.trimIndent()
                )
                .addModifiers(Modifier.PUBLIC)
                .returns(String::class.java)
                .addStatement($$"return $N", "name")
                .build()
        )

        addMethod(
            MethodSpec.methodBuilder("getSubGroups")
                .addJavadoc(
                    """
                Gets all emoji subgroups.

                @return All emoji subgroups.
            """.trimIndent()
                )
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(getParameterizedTypName(List::class.java, jemojiBasePackagePathString, "EmojiSubGroup"))
                .addStatement($$"return $N", "EMOJI_SUBGROUPS")
                .build()
        )

        addMethod(
            MethodSpec.methodBuilder("fromString")
                .addJavadoc(
                    """
                Gets the emoji subgroup for the given name.

                @param name The name of the emoji subgroup.
                @return The emoji subgroup.
            """.trimIndent()
                )
                .addParameter(String::class.java, "name", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(jemojiBasePackagePathString, "EmojiSubGroup"))
                .addCode(
                    CodeBlock.of(
                        """
                        for (final EmojiSubGroup emojiSubGroup : EMOJI_SUBGROUPS) {
                            if (emojiSubGroup.getName().equals(name)) {
                                return emojiSubGroup;
                             }
                        }
                        throw new IllegalArgumentException("No EmojiSubGroup found for name: " + name);
                    """.trimIndent()
                    )
                )
                .addAnnotation(JsonCreator::class.java)
                .build()
        )

        addMethod(
            MethodSpec.methodBuilder("getGroup")
                .addJavadoc(
                    """
                Gets the parent group this sub group belongs to.

                @return The parent group.
            """.trimIndent()
                )
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(jemojiBasePackagePathString, "EmojiGroup"))
                .addStatement($$"return $N", "emojiGroup")
                .build()
        )
    }.saveGeneratedJavaSourceFile(rootDir)
}

fun createStaticConstantsClassFromPreComputation(emojiArrayNode: ArrayNode, rootDir: String) {
    TypeSpec.classBuilder("PreComputedConstants").apply {
        addModifiers(Modifier.FINAL, Modifier.PUBLIC)
        addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", $$"$S", "unused").build())

        val variablesModifiers = listOf(
            Modifier.PUBLIC,
            Modifier.STATIC,
            Modifier.FINAL
        )

        addField(
            FieldSpec.builder(TypeName.INT, "MAXIMUM_EMOJI_URL_ENCODED_LENGTH", *variablesModifiers.toTypedArray())
                .initializer(
                    $$"$L",
                    emojiArrayNode.values().map { it.get("urlEncoded").asString() }.distinct()
                        .maxOfOrNull { it.codePointCount(0, it.length) }!!.toString()
                ).build()
        )

        addField(
            FieldSpec.builder(TypeName.INT, "MINIMUM_EMOJI_URL_ENCODED_LENGTH", *variablesModifiers.toTypedArray())
                .initializer(
                    $$"$L",
                    emojiArrayNode.values().map { it.get("urlEncoded").asString() }.distinct()
                        .minOfOrNull { it.codePointCount(0, it.length) }!!.toString()
                ).build()
        )

        addField(
            FieldSpec.builder(TypeName.INT, "ALIAS_EMOJI_MAX_LENGTH", *variablesModifiers.toTypedArray())
                .initializer(
                    $$"$L",
                    emojiArrayNode.values().flatMap { node ->
                        listOf("discordAliases", "githubAliases", "slackAliases").flatMap { key ->
                            node[key].values()?.map { it.asString() } ?: emptyList()
                        }
                    }.distinct().maxOfOrNull { it.codePointCount(0, it.length) }!!.toString()
                ).build()
        )

        addField(
            FieldSpec.builder(
                TypeName.INT,
                "MAX_HTML_DECIMAL_SINGLE_EMOJIS_CONCATENATED_LENGTH",
                *variablesModifiers.toTypedArray()
            ).initializer(
                $$"$L",
                emojiArrayNode.values().map { it.get("htmlDec").asString() }
                    .maxOfOrNull { it.chars().filter { ch: Int -> ch == ';'.code }.count() }!!.toInt()
            ).build()
        )

        addField(
            FieldSpec.builder(TypeName.INT, "MIN_HTML_DECIMAL_CODEPOINT_LENGTH", *variablesModifiers.toTypedArray())
                .initializer(
                    $$"$L",
                    emojiArrayNode.values().map { it.get("htmlDec").asString() }
                        .minOfOrNull { text: String -> text.codePoints().toArray().size }!!.toString()
                ).build()
        )

        emojiArrayNode.asSequence()
            .flatMap { node ->
                listOf("discordAliases", "githubAliases", "slackAliases").flatMap { key ->
                    node[key].values()?.map { it.asString() } ?: emptyList()
                }
            }
            .distinct()
            .map { it.codePointAt(0) }
            .distinct()
            .sorted()
            .toList()
            .let {
                addField(
                    FieldSpec.builder(
                        getParameterizedTypName(Set::class.java, "java.lang", "Integer"),
                        "POSSIBLE_EMOJI_ALIAS_STARTER_CODEPOINTS",
                        *variablesModifiers.toTypedArray()
                    ).initializer(
                        CodeBlock.of($$"new $T<>($T.asList(" + it.joinToString(", ") { $$"$L" } + "))",
                            HashSet::class.java,
                            Arrays::class.java,
                            *it.toTypedArray())
                    ).build()
                )
            }

        emojiArrayNode.asSequence().map { it.get("urlEncoded").asString() }
            .map { it.codePointAt(0) }
            .distinct()
            .sorted()
            .toList()
            .let {
                addField(
                    FieldSpec.builder(
                        getParameterizedTypName(Set::class.java, "java.lang", "Integer"),
                        "POSSIBLE_EMOJI_URL_ENCODED_STARTER_CODEPOINTS",
                        *variablesModifiers.toTypedArray()
                    ).initializer(
                        CodeBlock.of($$"new $T<>($T.asList(" + it.joinToString(", ") { $$"$L" } + "))",
                            HashSet::class.java,
                            Arrays::class.java,
                            *it.toTypedArray())
                    ).build()
                )
            }

        emojiArrayNode.asSequence().map { it.get("urlEncoded").asString() }
            .flatMap { it.split("%") }
            .distinct()
            .filter { it.isNotEmpty() }
            .sorted()
            .toList()
            .let {
                addField(
                    FieldSpec.builder(
                        getParameterizedTypName(Set::class.java, String::class.java),
                        "ALLOWED_EMOJI_URL_ENCODED_SEQUENCES",
                        *variablesModifiers.toTypedArray()
                    )
                        .initializer(
                            CodeBlock.of($$"new $T<>($T.asList(" + it.joinToString(", ") { $$"$S" } + "))",
                                HashSet::class.java,
                                Arrays::class.java,
                                *it.toTypedArray())
                        ).build()
                )
            }
    }.saveGeneratedJavaSourceFile(rootDir, "internal")
}

fun createSubGroupEmojiInterface(
    emojiSubgroupFileName: String,
    emojiSubGroupInterfaceConstantVariables: List<FieldSpec>,
    rootDir: String
) {
    TypeSpec.interfaceBuilder(emojiSubgroupFileName).apply {
        addModifiers(Modifier.PUBLIC)
        addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", $$"$S", "unused").build())
        addFields(emojiSubGroupInterfaceConstantVariables)
    }.saveGeneratedJavaSourceFile(rootDir, "generated")
}

fun TypeSpec.Builder.saveGeneratedJavaSourceFile(rootDir: String, subPackage: String? = null) {
    val generatedSourcesDir = "$rootDir/jemoji/build/generated/jemoji"
    addAnnotation(
        AnnotationSpec.builder(ClassName.get("javax.annotation.processing", "Generated"))
            .addMember("value", $$"$S", "emoji-generator")
            .build()
    )
    val packageName = jemojiPackagePath.joinToString(".") + (subPackage?.let { ".$it" } ?: "")
    JavaFile.builder(packageName, this.build())
        .indent("    ")
        .skipJavaLangImports(true)
        .build()
        .writeTo(File(generatedSourcesDir).toPath())
}


fun getParameterizedTypName(clazz: Class<*>, clazz2: Class<*>): ParameterizedTypeName =
    ParameterizedTypeName.get(ClassName.get(clazz), ClassName.get(clazz2))

fun getParameterizedTypName(clazz: Class<*>, packageName: String, simpleName: String): ParameterizedTypeName =
    ParameterizedTypeName.get(ClassName.get(clazz), ClassName.get(packageName, simpleName))

fun emojiDescriptionToConstantName(description: String, qualification: String): String {
    return (description
        .uppercase()
        .replace(descriptionReplaceStarRegex, "STAR")
        .replace(descriptionReplaceHashRegex, "HASH")
        .replace(descriptionReplaceRegex, "_")
        .replace(
            descriptionReplaceEndingUnderscoreRegex,
            ""
        ) + (if (qualification != "fully-qualified") "_" + qualification.uppercase()
        .replace(descriptionReplaceRegex, "_") else "")).let {
// Special cases for emoji names that start with 1st, 2nd, 3rd
        if (it.startsWith("1ST")) {
            "FIRST" + it.substring(3)
        } else if (it.startsWith("2ND")) {
            "SECOND" + it.substring(3)
        } else if (it.startsWith("3RD")) {
            "THIRD" + it.substring(3)
        } else {
            it
        }
    }
}

fun emojiDescriptionToFileName(description: String): String {
    return "Emoji" + description
        .split(" ").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .split("&")
        .joinToString("And") { it.replaceFirstChar(Char::uppercaseChar) }
}

fun emojiGroupToEnumName(group: String): String {
    return group.uppercase().replace("-", "_").replace("&", "AND").replace(" ", "_").replace("__", "_")
}
