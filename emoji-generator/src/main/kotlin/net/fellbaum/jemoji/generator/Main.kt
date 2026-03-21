package net.fellbaum.jemoji.generator

import net.fellbaum.jemoji.generator.model.GradleEmoji
import net.fellbaum.jemoji.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import tools.jackson.module.kotlin.treeToValue
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val generateAll = args.contains("--all")
    val rootDir = args.firstOrNull { it.startsWith("--rootDir=") }
        ?.removePrefix("--rootDir=")
        ?: System.getProperty("user.dir")
    generate(rootDir, generateAll)
}

// NOTE: rootDir comes BEFORE generateAll so the default value on generateAll is valid Kotlin
fun generate(rootDir: String, generateAll: Boolean = false) {
    val client = OkHttpClient()
    val jacksonMapper = jacksonObjectMapper()

    val githubEmojiToAliasMap = getGithubEmojiAliasMap(client, jacksonMapper)
    val discordAliases = retrieveDiscordEmojiShortcutsFile()
    val slackAliases = retrieveSlackEmojiShortcutsFile()

    val githubEmojiDefinition = File("$rootDir/emoji_source_files/github-emoji-definition.json")
    githubEmojiDefinition.writeText(jacksonMapper.writeValueAsString(githubEmojiToAliasMap))

    val unicodeVariationLines = GitHubUnicodeData.getGitHubLatestEmojiVariationSequencesData()

    val emojisThatHaveVariations = unicodeVariationLines.lines()
        .asSequence()
        .filter { !it.startsWith("#") }
        .filter { it.isNotEmpty() }
        .map { it.split(";") }
        .map { it[0].split(" ")[0].trim() }
        .distinct()
        .map { String(Character.toChars(it.toInt(16))) }
        .toSet()

    val unicodeLines = GitHubUnicodeData.getGitHubLatestEmojiTestData()

    // drop the first block as it's just the header
    val allUnicodeEmojis = unicodeLines.split("# group: ").drop(1).flatMap { group ->

//      "group" is a string containing everything from a group which looks like:
//      # group: Smileys & Emotion
//      # subgroup: face-smiling
//      1F600     ; fully-qualified     # 😀 E0.6 grinning face
//
//      # subgroup: face-affection
//      1F970     ; fully-qualified     # 🥰 E11.0 smiling face with hearts


//      "groupSplit" is a list containing the group name and all subgroups
//      [0] = group name
//      [1] = subgroup 1
//      [2] = subgroup 2

        val groupSplit = group.split("# subgroup: ")

        // Get the first line of the group which is the group name and ignore the rest as they are just empty lines
        val groupName = groupSplit[0].lines()[0]

        groupSplit.drop(1).flatMap { subGroup ->

//          "subGroup" is a string containing everything from a subgroup which looks like:
//          face-smiling
//          1F600     ; fully-qualified     # 😀 E0.6 grinning face
//
//          face-affection
//          1F970     ; fully-qualified     # 🥰 E11.0 smiling face with hearts
            val subGroupLines = subGroup.lines()
            val subGroupName = subGroupLines[0]
            //println(subGroupName)
            subGroupLines.asSequence().drop(1)
                .filter { !it.startsWith("#") && it.isNotBlank() }
                .map { it.split(";") }
                .map { stringList ->
                    // 1F44D     ; fully-qualified     # 👍 E0.6 thumbs up
                    //[   [0]    ][                [1]                   ]

                    val codepointsString = stringList[0].trim()
                        .split(" ")
                        .joinToString("") { String(Character.toChars(it.toInt(16))) }

                    //  fully-qualified     # 👍 E0.6 thumbs up
                    //[        [0]          ][      [1]       ]
                    // limit split to 1 because of the # keycap emoji description
                    val information = stringList[1].split("#", limit = 2)
                    val qualification = information[0].trim()

                    // 👍 E0.6 thumbs up
                    // [0] [1] [2]
                    val otherInformation = information[1].trim().split(" ", limit = 3)
                    val version = otherInformation[1].removePrefix("E").toDouble()
                    val emojiDescription = otherInformation[2].trim()

                    val completeDiscordAliases = buildSet {
                        discordAliases[codepointsString]?.let { addAll(it) }
                    }
                    val completeGitHubAliases = buildSet {
                        githubEmojiToAliasMap[codepointsString]?.let { alias ->
                            addAll(alias)
                        }
                    }
                    val completeSlackAliases = buildSet {
                        slackAliases[codepointsString]?.let { pairList ->
                            addAll(pairList)
                        }
                    }

                    GradleEmoji(
                        codepointsString,
                        //Get each char and fill with leading 0 as the representation is: \u0000
                        codepointsString.asSequence().joinToString(separator = "") { "\\u%04X".format(it.code) },
                        codepointsString.codePoints().mapToObj { operand -> "&#$operand" }
                            .collect(Collectors.joining(";")) + ";",
                        codepointsString.codePoints()
                            .mapToObj { operand -> "&#x" + Integer.toHexString(operand).uppercase() }
                            .collect(Collectors.joining(";")) + ";",
                        URLEncoder.encode(codepointsString, StandardCharsets.UTF_8.toString()),
                        completeDiscordAliases,
                        completeGitHubAliases,
                        completeSlackAliases,
                        Fitzpatrick.isFitzpatrickEmoji(codepointsString),
                        HairStyle.isHairStyleEmoji(codepointsString),
                        version,
                        qualification,
                        emojiDescription,
                        groupName,
                        subGroupName,
                        emojisThatHaveVariations.contains(codepointsString),
                        emptySet<String>()
                    )
                }.toList()
        }
    }


    ////////////////////////
    ////////////////////////
    ////////////////////////
    val repo = "unicode-org/cldr-json"
    val httpBuilderAnnotationsDerivedDirectory =
        "https://api.github.com/repos/${repo}/contents/cldr-json/cldr-annotations-derived-full/annotationsDerived".toHttpUrl()
            .newBuilder()
    val requestBuilder: Request.Builder = Request.Builder().addHeader("Accept", "application/vnd.github.raw+json")
    requestBuilder.url(httpBuilderAnnotationsDerivedDirectory.build())
    val descriptionDirectory: List<Map<String, Any>> =
        jacksonMapper.readValue<List<Map<String, Any>>>(client.newCall(requestBuilder.build()).execute().body.string())

    // For some reason, the Unicode GitHub repository has unqualified > minimally qualified emojis > fully qualified emojis as keys.
    // So there might be a description or keywords for the unqualified version but not the fully-qualified
    val descriptionNodesLanguageMap = mutableMapOf<String, MutableMap<String, String?>>()
    val keywordsNodesLanguageMap = mutableMapOf<String, MutableMap<String, List<String>?>>()
    val fileNameList = mutableListOf<String>()
    val emojisGroupedByDescription = allUnicodeEmojis.groupBy { it.description }

    val currentIndex = AtomicInteger(0)
    descriptionDirectory.parallelStream().forEach { directory ->
        val index = currentIndex.andIncrement
        if (index % 10 == 0) {
            println("$index / ${descriptionDirectory.size} description files processed")
        }
        val dirName = directory["name"] as String
        val descriptionNodeOutput = JsonNodeFactory.instance.objectNode()
        val keywordsNodeOutput = JsonNodeFactory.instance.objectNode()
        listOf(
            "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/cldr-annotations-derived-full/annotationsDerived/$dirName/annotations.json",
            "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/cldr-annotations-full/annotations/$dirName/annotations.json"
        ).forEach { url ->
            requestCLDREmojiDescriptionTranslation(
                url,
                client,
                jacksonMapper,
                descriptionNodeOutput,
                keywordsNodeOutput,
                dirName,
                rootDir
            )
        }

        val fileOutputDir = File("$rootDir/jemoji-languages")

        val descriptionMap: MutableMap<String, String> =
            jacksonMapper.treeToValue<MutableMap<String, String>>(descriptionNodeOutput)
        // Emojis should also have a distinct description by emoji.
        // So fully-qualified, minimally-qualified and unqualified emojis are basically the same except for the Unicode.
        descriptionMap.toMap().forEach { (_, value) ->
            emojisGroupedByDescription[value]?.forEach { emoji ->
                if (!descriptionMap.containsKey(emoji.emoji)) {
                    descriptionMap[emoji.emoji] = value
                }
            }
        }

        "$fileOutputDir/src/main/resources/emoji_sources/description/${dirName}.json".let {

            if (generateAll) {
                jacksonMapper.writeValue(File(it), descriptionMap)
            }
            descriptionNodesLanguageMap.put(dirName, descriptionMap as MutableMap<String, String?>)
        }

        // Emoji to List<keywords>
        val keywordMap: MutableMap<String, List<String>?> = jacksonMapper.treeToValue(keywordsNodeOutput)
        "$fileOutputDir/src/main/resources/emoji_sources/keyword/${dirName}.json".let {
            if (generateAll) {
                jacksonMapper.writeValue(File(it), descriptionMap)
            }
            keywordsNodesLanguageMap[dirName] = keywordMap
            if (dirName == "en") {
                keywordMap.forEach { (key, value) ->
                    for (entry in emojisGroupedByDescription.entries) {
                        if (entry.value.find { it.emoji == key } != null) {
                            entry.value.forEach { em -> em.keywords = value?.toSet()!! }
                            break
                        }
                    }
                }
            }
        }

        fileNameList.add(dirName)
    }

    ////////////////////////
    ////////////////////////
    ////////////////////////


    val jemojiResourcesFile = File("$rootDir/jemoji/src/main/resources/jemoji/emojis.json")
    val mapper = jacksonMapperBuilder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build()
    jemojiResourcesFile.writeText(mapper.writeValueAsString(allUnicodeEmojis))
    ////////////////////////
    ////////////////////////
    ////////////////////////
    writeContentToPublicFiles("emojis", allUnicodeEmojis, rootDir)

    if (generateAll) {
        for (emoji in allUnicodeEmojis) {
            val descriptionMap = buildMap {
                descriptionNodesLanguageMap.forEach { (key, value) ->
                    if (value.containsKey(emoji.emoji)) {
                        put(key, value[emoji.emoji])
                    } else {
                        put(key, null)
                    }
                }
            }
            emoji.description = descriptionMap

            val keywordsMap = buildMap {
                keywordsNodesLanguageMap.forEach { (key, value) ->
                    if (value.containsKey(emoji.emoji)) {
                        put(key, value[emoji.emoji])
                    } else {
                        put(key, null)
                    }
                }
            }
            emoji.keywords = keywordsMap
        }

        writeContentToPublicFiles("emojis-full", allUnicodeEmojis, rootDir)

        descriptionNodesLanguageMap.toMap().run {
            allUnicodeEmojis.forEach { emoji ->
                this.forEach { (locale, _) ->
                    descriptionNodesLanguageMap[locale]?.apply {
                        putIfAbsent(emoji.emoji, null)
                    }
                }
            }
        }
        keywordsNodesLanguageMap.toMap().run {
            allUnicodeEmojis.forEach { emoji ->
                this.forEach { (locale, _) ->
                    keywordsNodesLanguageMap[locale]?.apply {
                        putIfAbsent(emoji.emoji, null)
                    }
                }
            }
        }

        descriptionNodesLanguageMap.forEach { writeContentToPublicFiles("description/${it.key}", it.value, rootDir) }
        keywordsNodesLanguageMap.forEach { writeContentToPublicFiles("keywords/${it.key}", it.value, rootDir) }
    }

    generateEmojiLanguageEnum(fileNameList, rootDir)
    generateEmojiGroupEnum(allUnicodeEmojis.map { it.group }, rootDir)
    generateEmojiSubGroupEnum(allUnicodeEmojis.map { Pair(it.subgroup, it.group) }, rootDir)
    generateJavaSourceFiles(rootDir)
}
