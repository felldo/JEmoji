import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.jcabi.github.Coordinates
import com.jcabi.github.RtGitHub
import com.palantir.javapoet.*
import net.fellbaum.jemoji.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.html.HtmlPage
import org.htmlunit.html.HtmlScript
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Constructor
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import javax.lang.model.element.Modifier
import kotlin.math.ceil

plugins {
    id("base")
    alias(libs.plugins.versions.catalog)
}

tasks.register("publishAll") {
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "publish" })
    }
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.jackson.databind)
        classpath(libs.jackson.module.kotlin)
        classpath(libs.jackson.datatype.jdk8)
        classpath(libs.okhttp)
        classpath(libs.jsoup)
        classpath(libs.htmlunit)
        classpath(libs.javapoet)
        classpath(libs.kotlinx.coroutines.core)
        classpath(libs.kotlinx.coroutines.jdk8)
        classpath("com.esotericsoftware.kryo:kryo5:5.6.2")
        classpath(files(project.rootDir.path + "\\libs\\jemoji.jar"))
        classpath("com.jcabi:jcabi-github:1.10.0")
    }
}

// https://github.com/unicode-org/cldr/tree/main/common/annotations
// https://github.com/unicode-org/cldr-json/tree/main/cldr-json
// https://stackoverflow.com/questions/39490865/how-can-i-get-the-full-list-of-slack-emoji-through-api
// https://github.com/iamcal/emoji-data/
fun requestCLDREmojiDescriptionTranslation(
    url: String,
    client: OkHttpClient,
    mapper: ObjectMapper,
    descriptionNodeOutput: ObjectNode,
    keywordsNodeOutput: ObjectNode,
    fileName: String
) {
    val fileHttpBuilder: HttpUrl.Builder = url.toHttpUrl().newBuilder()
    val requestBuilder: Request.Builder = Request.Builder().addHeader("Accept", "application/vnd.github.raw+json")
    requestBuilder.url(fileHttpBuilder.build())

    val fileContent: JsonNode = mapper.readTree(client.newCall(requestBuilder.build()).execute().body.string())

    val translationFile =
        File("$rootDir/emoji_source_files/description/$fileName${if (fileContent.has("annotationsDerived")) "-derieved" else ""}.json")
    translationFile.writeText(mapper.writeValueAsString(fileContent))

    val annotationsDerived =
        if (fileContent.has("annotations")) fileContent.get("annotations") else fileContent.get("annotationsDerived")
    if (!annotationsDerived.has("annotations")) {
        return
    }

    val annotationsNode = annotationsDerived.get("annotations")


    annotationsNode.properties().forEach {
        if (it.value.has("tts")) {
            descriptionNodeOutput.put(it.key, it.value.get("tts").joinToString(" ") { jsonNode -> jsonNode.asText() })
            val keywordsArray = keywordsNodeOutput.putArray(it.key)
            if (it.value.has("default")) {
                it.value.get("default").forEach { jsonNode -> keywordsArray.add(jsonNode) }
            }
        }
    }
}

val jemojiPackagePath = listOf("net", "fellbaum", "jemoji")
val jemojiBasePackagePathString = jemojiPackagePath.joinToString(".")
tasks.register("generate") {
    group = "jemoji"
    doFirst {
        generate(false)
    }
}

tasks.register("generateAll") {
    group = "jemoji"
    doFirst {
        generate(true)
    }
}

object GitHubUnicodeData {
    val github = RtGitHub()
    val coordinates = Coordinates.Simple("unicode-org", "unicodetools")

    fun getGitHubLatestEmojiTestData(): String {
        val pathPrefix = "unicodetools/data/emoji/"
        val latestVersion = github.repos().get(coordinates)
            .contents()
            .iterate(pathPrefix, "main")
            .map { it.path() }
            .map { it.replace(pathPrefix, "") }
            .filter { it.matches(Regex("[0-9]+\\.[0-9+]")) }
            .map { it.toDoubleOrNull() }
            .sortedBy { it }
            .toSet().last()

        return github.repos().get(coordinates)
            .contents().get("$pathPrefix$latestVersion/emoji-test.txt").raw().readBytes().decodeToString()
    }

    fun getGitHubLatestEmojiVariationSequencesData(): String {
        val pathPrefix = "unicodetools/data/ucd/"
        val latestVersion = github.repos().get(coordinates)
            .contents()
            .iterate(pathPrefix, "main")
            .map { it.path() }
            .map { it.replace(pathPrefix, "") }
            .filter { it.matches(Regex("^[0-9]+(\\.[0-9+])+$")) }
            .sortedWith(
                compareBy(
                    { it.split(".")[0].toInt() },
                    { it.split(".")[1].toInt() },
                    { it.split(".")[2].toInt() }
                )
            )
            .toSet().last()

        return github.repos().get(coordinates)
            .contents().get("$pathPrefix$latestVersion/emoji/emoji-variation-sequences.txt").raw().readBytes()
            .decodeToString()
    }
}

fun generate(generateAll: Boolean = false) {
    val client = OkHttpClient()
    val mapper = jacksonObjectMapper()

    val githubEmojiToAliasMap = getGithubEmojiAliasMap(client, mapper)
    val discordAliases = retrieveDiscordEmojiShortcutsFile()
    val slackAliases = retrieveSlackEmojiShortcutsFile()

    val githubEmojiDefinition = File("$rootDir/emoji_source_files/github-emoji-definition.json")
    githubEmojiDefinition.writeText(mapper.writeValueAsString(githubEmojiToAliasMap))

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
    val httpBuilderAnnotationsDerivedDirectory: HttpUrl.Builder =
        "https://api.github.com/repos/${repo}/contents/cldr-json/cldr-annotations-derived-full/annotationsDerived".toHttpUrl()
            .newBuilder()
    val requestBuilder: Request.Builder = Request.Builder().addHeader("Accept", "application/vnd.github.raw+json")
    requestBuilder.url(httpBuilderAnnotationsDerivedDirectory.build())
    val descriptionDirectory: List<Map<String, Any>> =
        mapper.readValue(client.newCall(requestBuilder.build()).execute().body.string())
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
                mapper,
                descriptionNodeOutput,
                keywordsNodeOutput,
                dirName
            )
        }

        val fileOutputDir = project(":jemoji-languages").projectDir

        val descriptionMap: MutableMap<String, String> = mapper.treeToValue(descriptionNodeOutput)
        // Emojis should also have a distinct description by emoji.
        // So fully-qualified, minimally-qualified and unqualified emojis are basically the same except for the Unicode.
        descriptionMap.toMap().forEach { (_, value) ->
            emojisGroupedByDescription[value]?.forEach { emoji ->
                if (!descriptionMap.containsKey(emoji.emoji)) {
                    descriptionMap[emoji.emoji] = value
                }
            }
        }

        "$fileOutputDir/src/main/resources/emoji_sources/description/${dirName}.ser".let {
            if (generateAll) {
                FileOutputStream(it).use { fos -> ObjectOutputStream(fos).use { oos -> oos.writeObject(descriptionMap) } }
            }
            descriptionNodesLanguageMap.put(dirName, descriptionMap as MutableMap<String, String?>)
        }

        // Emoji to List<keywords>
        val keywordMap: MutableMap<String, List<String>?> = mapper.treeToValue(keywordsNodeOutput)
        //val keywordMap: Map<String, List<String>> = mapper.treeToValue(keywordsNodeOutput, object : TypeReference<Map<String, List<String>>>() {})
        "$fileOutputDir/src/main/resources/emoji_sources/keyword/${dirName}.ser".let {
            if (generateAll) {
                FileOutputStream(it).use { fos -> ObjectOutputStream(fos).use { oos -> oos.writeObject(keywordMap) } }
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


    project(":jemoji").layout.projectDirectory.dir("src/main/resources/jemoji/serializedEmojis.ser").asFile.let { file ->
        val clazz = Emoji::class.java
        val constructor = clazz.declaredConstructors[0] as Constructor<Emoji>
        constructor.isAccessible = true

        val emojiMap: HashMap<String, Emoji> = HashMap<String, Emoji>()

        allUnicodeEmojis.forEach { emoji ->
            emojiMap[emoji.emoji] = constructor.newInstance(
                emoji.emoji,
                emoji.unicode,
                emoji.htmlDec,
                emoji.htmlHex,
                emoji.urlEncoded,
                emoji.discordAliases.stream().collect(Collectors.toList()),
                emoji.slackAliases.stream().collect(Collectors.toList()),
                emoji.githubAliases.stream().collect(Collectors.toList()),
                Collections.emptyList<String>(),
                emoji.hasFitzpatrick,
                emoji.hasHairStyle,
                emoji.version,
                Qualification.fromString(emoji.qualification),
                emoji.description as String,
                EmojiGroup.fromString(emoji.group),
                EmojiSubGroup.fromString(emoji.subgroup),
                emoji.hasVariationSelectors
            )
        }

        FileOutputStream(file).use { ObjectOutputStream(it).use { it.writeObject(emojiMap) } }


        /*val kryo: Kryo = Kryo()
        kryo.register(HashMap::class.java)
        kryo.register(Emoji::class.java, JavaSerializer())
        kryo.register(EmojiGroup::class.java, JavaSerializer())
        kryo.register(EmojiSubGroup::class.java, JavaSerializer())
        kryo.register(Qualification::class.java, JavaSerializer())

        // Serialisierung
        val byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        val output = Output(FileOutputStream(project(":jemoji").layout.projectDirectory.dir("src/main/resources/jemoji/kryo.bin").asFile))
        kryo.writeObject(output, emojiMap)
        output.close()*/
    }

////////////////////////
////////////////////////
////////////////////////
    writeContentToPublicFiles("emojis", allUnicodeEmojis)

    if (generateAll) {
        for (emoji in allUnicodeEmojis) {
            val descriptionMap = buildMap<String, String?> {
                descriptionNodesLanguageMap.forEach { (key, value) ->
                    if (value.containsKey(emoji.emoji)) {
                        put(key, value[emoji.emoji])
                    } else {
                        put(key, null)
                    }
                }
            }
            emoji.description = descriptionMap

            val keywordsMap = buildMap<String, List<String>?> {
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

        writeContentToPublicFiles("emojis-full", allUnicodeEmojis)

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

        descriptionNodesLanguageMap.forEach { writeContentToPublicFiles("description/${it.key}", it.value) }
        keywordsNodesLanguageMap.forEach { writeContentToPublicFiles("keywords/${it.key}", it.value) }
    }

    generateEmojiLanguageEnum(fileNameList)
    generateEmojiGroupEnum(allUnicodeEmojis.map { it.group })
    generateEmojiSubGroupEnum(allUnicodeEmojis.map { Pair(it.subgroup, it.group) })
    generateJavaSourceFiles()
}

fun writeContentToPublicFiles(fileName: String, content: Any) {
    val publicFile = File("$rootDir/public/$fileName.json")
    val publicFileMin = File("$rootDir/public/$fileName.min.json")
    val mapper =
        jacksonObjectMapper().registerModule(Jdk8Module()).enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    publicFile.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(content))
    publicFileMin.writeText(mapper.writeValueAsString(content))
}

fun getGithubEmojiAliasMap(client: OkHttpClient, mapper: ObjectMapper): Map<String, MutableList<String>> {
    val gitHubEmojiAliasUrl = "https://api.github.com/emojis"

    val json = client.newCall(Request.Builder().url(gitHubEmojiAliasUrl).build()).execute().body.string()

    val githubEmojiAliasMap = mapper.readTree(json)
        .properties()
        .asSequence()
        .map { it.key to it.value.asText().uppercase() }
        .filter {
            it.second.contains(
                "https://github.githubassets.com/images/icons/emoji/unicode",
                ignoreCase = true
            )
        }
        .map {
            getStringWithColon(it.first) to it.second.replace(
                "https://github.githubassets.com/images/icons/emoji/unicode/",
                "",
                true
            ).replace(".png?v8", "", true)
        }
        .groupBy { it.second }
    //{1F44D=[(+1, 1F44D), (thumbsup, 1F44D)]


    val fetchedGhList =
        "[" + githubEmojiAliasMap.entries.joinToString(", ") { entry -> entry.value.joinToString(", ") { "\"" + it.first + "\"" } } + "]"

    val currentKeywordListGitHub: List<String> = jacksonObjectMapper().readValue(
        "[\":+1:\",\":thumbsup:\",\":-1:\",\":thumbsdown:\",\":100:\",\":1234:\",\":1st_place_medal:\",\":2nd_place_medal:\",\":3rd_place_medal:\",\":8ball:\",\":a:\",\":ab:\",\":abacus:\",\":abc:\",\":abcd:\",\":accept:\",\":accordion:\",\":adhesive_bandage:\",\":adult:\",\":aerial_tramway:\",\":afghanistan:\",\":airplane:\",\":aland_islands:\",\":alarm_clock:\",\":albania:\",\":alembic:\",\":algeria:\",\":alien:\",\":ambulance:\",\":american_samoa:\",\":amphora:\",\":anatomical_heart:\",\":anchor:\",\":andorra:\",\":angel:\",\":anger:\",\":angola:\",\":angry:\",\":anguilla:\",\":anguished:\",\":ant:\",\":antarctica:\",\":antigua_barbuda:\",\":apple:\",\":aquarius:\",\":argentina:\",\":aries:\",\":armenia:\",\":arrow_backward:\",\":arrow_double_down:\",\":arrow_double_up:\",\":arrow_down:\",\":arrow_down_small:\",\":arrow_forward:\",\":arrow_heading_down:\",\":arrow_heading_up:\",\":arrow_left:\",\":arrow_lower_left:\",\":arrow_lower_right:\",\":arrow_right:\",\":arrow_right_hook:\",\":arrow_up:\",\":arrow_up_down:\",\":arrow_up_small:\",\":arrow_upper_left:\",\":arrow_upper_right:\",\":arrows_clockwise:\",\":arrows_counterclockwise:\",\":art:\",\":articulated_lorry:\",\":artificial_satellite:\",\":artist:\",\":aruba:\",\":ascension_island:\",\":asterisk:\",\":astonished:\",\":astronaut:\",\":athletic_shoe:\",\":atm:\",\":atom_symbol:\",\":australia:\",\":austria:\",\":auto_rickshaw:\",\":avocado:\",\":axe:\",\":azerbaijan:\",\":b:\",\":baby:\",\":baby_bottle:\",\":baby_chick:\",\":baby_symbol:\",\":back:\",\":bacon:\",\":badger:\",\":badminton:\",\":bagel:\",\":baggage_claim:\",\":baguette_bread:\",\":bahamas:\",\":bahrain:\",\":balance_scale:\",\":bald_man:\",\":bald_woman:\",\":ballet_shoes:\",\":balloon:\",\":ballot_box:\",\":ballot_box_with_check:\",\":bamboo:\",\":banana:\",\":bangbang:\",\":bangladesh:\",\":banjo:\",\":bank:\",\":bar_chart:\",\":barbados:\",\":barber:\",\":baseball:\",\":basket:\",\":basketball:\",\":basketball_man:\",\":bouncing_ball_man:\",\":basketball_woman:\",\":bouncing_ball_woman:\",\":bat:\",\":bath:\",\":bathtub:\",\":battery:\",\":beach_umbrella:\",\":beans:\",\":bear:\",\":bearded_person:\",\":beaver:\",\":bed:\",\":bee:\",\":honeybee:\",\":beer:\",\":beers:\",\":beetle:\",\":beginner:\",\":belarus:\",\":belgium:\",\":belize:\",\":bell:\",\":bell_pepper:\",\":bellhop_bell:\",\":benin:\",\":bento:\",\":bermuda:\",\":beverage_box:\",\":bhutan:\",\":bicyclist:\",\":bike:\",\":biking_man:\",\":biking_woman:\",\":bikini:\",\":billed_cap:\",\":biohazard:\",\":bird:\",\":birthday:\",\":bison:\",\":biting_lip:\",\":black_bird:\",\":black_cat:\",\":black_circle:\",\":black_flag:\",\":black_heart:\",\":black_joker:\",\":black_large_square:\",\":black_medium_small_square:\",\":black_medium_square:\",\":black_nib:\",\":black_small_square:\",\":black_square_button:\",\":blond_haired_man:\",\":blond_haired_person:\",\":blond_haired_woman:\",\":blonde_woman:\",\":blossom:\",\":blowfish:\",\":blue_book:\",\":blue_car:\",\":blue_heart:\",\":blue_square:\",\":blueberries:\",\":blush:\",\":boar:\",\":boat:\",\":sailboat:\",\":bolivia:\",\":bomb:\",\":bone:\",\":book:\",\":open_book:\",\":bookmark:\",\":bookmark_tabs:\",\":books:\",\":boom:\",\":collision:\",\":boomerang:\",\":boot:\",\":bosnia_herzegovina:\",\":botswana:\",\":bouncing_ball_person:\",\":bouquet:\",\":bouvet_island:\",\":bow:\",\":bow_and_arrow:\",\":bowing_man:\",\":bowing_woman:\",\":bowl_with_spoon:\",\":bowling:\",\":boxing_glove:\",\":boy:\",\":brain:\",\":brazil:\",\":bread:\",\":breast_feeding:\",\":bricks:\",\":bride_with_veil:\",\":woman_with_veil:\",\":bridge_at_night:\",\":briefcase:\",\":british_indian_ocean_territory:\",\":british_virgin_islands:\",\":broccoli:\",\":broken_heart:\",\":broom:\",\":brown_circle:\",\":brown_heart:\",\":brown_square:\",\":brunei:\",\":bubble_tea:\",\":bubbles:\",\":bucket:\",\":bug:\",\":building_construction:\",\":bulb:\",\":bulgaria:\",\":bullettrain_front:\",\":bullettrain_side:\",\":burkina_faso:\",\":burrito:\",\":burundi:\",\":bus:\",\":business_suit_levitating:\",\":busstop:\",\":bust_in_silhouette:\",\":busts_in_silhouette:\",\":butter:\",\":butterfly:\",\":cactus:\",\":cake:\",\":calendar:\",\":call_me_hand:\",\":calling:\",\":cambodia:\",\":camel:\",\":camera:\",\":camera_flash:\",\":cameroon:\",\":camping:\",\":canada:\",\":canary_islands:\",\":cancer:\",\":candle:\",\":candy:\",\":canned_food:\",\":canoe:\",\":cape_verde:\",\":capital_abcd:\",\":capricorn:\",\":car:\",\":red_car:\",\":card_file_box:\",\":card_index:\",\":card_index_dividers:\",\":caribbean_netherlands:\",\":carousel_horse:\",\":carpentry_saw:\",\":carrot:\",\":cartwheeling:\",\":cat:\",\":cat2:\",\":cayman_islands:\",\":cd:\",\":central_african_republic:\",\":ceuta_melilla:\",\":chad:\",\":chains:\",\":chair:\",\":champagne:\",\":chart:\",\":chart_with_downwards_trend:\",\":chart_with_upwards_trend:\",\":checkered_flag:\",\":cheese:\",\":cherries:\",\":cherry_blossom:\",\":chess_pawn:\",\":chestnut:\",\":chicken:\",\":child:\",\":children_crossing:\",\":chile:\",\":chipmunk:\",\":chocolate_bar:\",\":chopsticks:\",\":christmas_island:\",\":christmas_tree:\",\":church:\",\":cinema:\",\":circus_tent:\",\":city_sunrise:\",\":city_sunset:\",\":cityscape:\",\":cl:\",\":clamp:\",\":clap:\",\":clapper:\",\":classical_building:\",\":climbing:\",\":climbing_man:\",\":climbing_woman:\",\":clinking_glasses:\",\":clipboard:\",\":clipperton_island:\",\":clock1:\",\":clock10:\",\":clock1030:\",\":clock11:\",\":clock1130:\",\":clock12:\",\":clock1230:\",\":clock130:\",\":clock2:\",\":clock230:\",\":clock3:\",\":clock330:\",\":clock4:\",\":clock430:\",\":clock5:\",\":clock530:\",\":clock6:\",\":clock630:\",\":clock7:\",\":clock730:\",\":clock8:\",\":clock830:\",\":clock9:\",\":clock930:\",\":closed_book:\",\":closed_lock_with_key:\",\":closed_umbrella:\",\":cloud:\",\":cloud_with_lightning:\",\":cloud_with_lightning_and_rain:\",\":cloud_with_rain:\",\":cloud_with_snow:\",\":clown_face:\",\":clubs:\",\":cn:\",\":coat:\",\":cockroach:\",\":cocktail:\",\":coconut:\",\":cocos_islands:\",\":coffee:\",\":coffin:\",\":coin:\",\":cold_face:\",\":cold_sweat:\",\":colombia:\",\":comet:\",\":comoros:\",\":compass:\",\":computer:\",\":computer_mouse:\",\":confetti_ball:\",\":confounded:\",\":confused:\",\":congo_brazzaville:\",\":congo_kinshasa:\",\":congratulations:\",\":construction:\",\":construction_worker:\",\":construction_worker_man:\",\":construction_worker_woman:\",\":control_knobs:\",\":convenience_store:\",\":cook:\",\":cook_islands:\",\":cookie:\",\":cool:\",\":cop:\",\":police_officer:\",\":copyright:\",\":coral:\",\":corn:\",\":costa_rica:\",\":cote_divoire:\",\":couch_and_lamp:\",\":couple:\",\":couple_with_heart:\",\":couple_with_heart_man_man:\",\":couple_with_heart_woman_man:\",\":couple_with_heart_woman_woman:\",\":couplekiss:\",\":couplekiss_man_man:\",\":couplekiss_man_woman:\",\":couplekiss_woman_woman:\",\":cow:\",\":cow2:\",\":cowboy_hat_face:\",\":crab:\",\":crayon:\",\":credit_card:\",\":crescent_moon:\",\":cricket:\",\":cricket_game:\",\":croatia:\",\":crocodile:\",\":croissant:\",\":crossed_fingers:\",\":crossed_flags:\",\":crossed_swords:\",\":crown:\",\":crutch:\",\":cry:\",\":crying_cat_face:\",\":crystal_ball:\",\":cuba:\",\":cucumber:\",\":cup_with_straw:\",\":cupcake:\",\":cupid:\",\":curacao:\",\":curling_stone:\",\":curly_haired_man:\",\":curly_haired_woman:\",\":curly_loop:\",\":currency_exchange:\",\":curry:\",\":cursing_face:\",\":custard:\",\":customs:\",\":cut_of_meat:\",\":cyclone:\",\":cyprus:\",\":czech_republic:\",\":dagger:\",\":dancer:\",\":woman_dancing:\",\":dancers:\",\":dancing_men:\",\":dancing_women:\",\":dango:\",\":dark_sunglasses:\",\":dart:\",\":dash:\",\":date:\",\":de:\",\":deaf_man:\",\":deaf_person:\",\":deaf_woman:\",\":deciduous_tree:\",\":deer:\",\":denmark:\",\":department_store:\",\":derelict_house:\",\":desert:\",\":desert_island:\",\":desktop_computer:\",\":detective:\",\":diamond_shape_with_a_dot_inside:\",\":diamonds:\",\":diego_garcia:\",\":disappointed:\",\":disappointed_relieved:\",\":disguised_face:\",\":diving_mask:\",\":diya_lamp:\",\":dizzy:\",\":dizzy_face:\",\":djibouti:\",\":dna:\",\":do_not_litter:\",\":dodo:\",\":dog:\",\":dog2:\",\":dollar:\",\":dolls:\",\":dolphin:\",\":flipper:\",\":dominica:\",\":dominican_republic:\",\":donkey:\",\":door:\",\":dotted_line_face:\",\":doughnut:\",\":dove:\",\":dragon:\",\":dragon_face:\",\":dress:\",\":dromedary_camel:\",\":drooling_face:\",\":drop_of_blood:\",\":droplet:\",\":drum:\",\":duck:\",\":dumpling:\",\":dvd:\",\":e-mail:\",\":email:\",\":eagle:\",\":ear:\",\":ear_of_rice:\",\":ear_with_hearing_aid:\",\":earth_africa:\",\":earth_americas:\",\":earth_asia:\",\":ecuador:\",\":egg:\",\":eggplant:\",\":egypt:\",\":eight:\",\":eight_pointed_black_star:\",\":eight_spoked_asterisk:\",\":eject_button:\",\":el_salvador:\",\":electric_plug:\",\":elephant:\",\":elevator:\",\":elf:\",\":elf_man:\",\":elf_woman:\",\":empty_nest:\",\":end:\",\":england:\",\":envelope:\",\":envelope_with_arrow:\",\":equatorial_guinea:\",\":eritrea:\",\":es:\",\":estonia:\",\":ethiopia:\",\":eu:\",\":european_union:\",\":euro:\",\":european_castle:\",\":european_post_office:\",\":evergreen_tree:\",\":exclamation:\",\":heavy_exclamation_mark:\",\":exploding_head:\",\":expressionless:\",\":eye:\",\":eye_speech_bubble:\",\":eyeglasses:\",\":eyes:\",\":face_exhaling:\",\":face_holding_back_tears:\",\":face_in_clouds:\",\":face_with_diagonal_mouth:\",\":face_with_head_bandage:\",\":face_with_open_eyes_and_hand_over_mouth:\",\":face_with_peeking_eye:\",\":face_with_spiral_eyes:\",\":face_with_thermometer:\",\":facepalm:\",\":facepunch:\",\":fist_oncoming:\",\":punch:\",\":factory:\",\":factory_worker:\",\":fairy:\",\":fairy_man:\",\":fairy_woman:\",\":falafel:\",\":falkland_islands:\",\":fallen_leaf:\",\":family:\",\":family_man_boy:\",\":family_man_boy_boy:\",\":family_man_girl:\",\":family_man_girl_boy:\",\":family_man_girl_girl:\",\":family_man_man_boy:\",\":family_man_man_boy_boy:\",\":family_man_man_girl:\",\":family_man_man_girl_boy:\",\":family_man_man_girl_girl:\",\":family_man_woman_boy:\",\":family_man_woman_boy_boy:\",\":family_man_woman_girl:\",\":family_man_woman_girl_boy:\",\":family_man_woman_girl_girl:\",\":family_woman_boy:\",\":family_woman_boy_boy:\",\":family_woman_girl:\",\":family_woman_girl_boy:\",\":family_woman_girl_girl:\",\":family_woman_woman_boy:\",\":family_woman_woman_boy_boy:\",\":family_woman_woman_girl:\",\":family_woman_woman_girl_boy:\",\":family_woman_woman_girl_girl:\",\":farmer:\",\":faroe_islands:\",\":fast_forward:\",\":fax:\",\":fearful:\",\":feather:\",\":feet:\",\":paw_prints:\",\":female_detective:\",\":female_sign:\",\":ferris_wheel:\",\":ferry:\",\":field_hockey:\",\":fiji:\",\":file_cabinet:\",\":file_folder:\",\":film_projector:\",\":film_strip:\",\":finland:\",\":fire:\",\":fire_engine:\",\":fire_extinguisher:\",\":firecracker:\",\":firefighter:\",\":fireworks:\",\":first_quarter_moon:\",\":first_quarter_moon_with_face:\",\":fish:\",\":fish_cake:\",\":fishing_pole_and_fish:\",\":fist:\",\":fist_raised:\",\":fist_left:\",\":fist_right:\",\":five:\",\":flags:\",\":flamingo:\",\":flashlight:\",\":flat_shoe:\",\":flatbread:\",\":fleur_de_lis:\",\":flight_arrival:\",\":flight_departure:\",\":floppy_disk:\",\":flower_playing_cards:\",\":flushed:\",\":flute:\",\":fly:\",\":flying_disc:\",\":flying_saucer:\",\":fog:\",\":foggy:\",\":folding_hand_fan:\",\":fondue:\",\":foot:\",\":football:\",\":footprints:\",\":fork_and_knife:\",\":fortune_cookie:\",\":fountain:\",\":fountain_pen:\",\":four:\",\":four_leaf_clover:\",\":fox_face:\",\":fr:\",\":framed_picture:\",\":free:\",\":french_guiana:\",\":french_polynesia:\",\":french_southern_territories:\",\":fried_egg:\",\":fried_shrimp:\",\":fries:\",\":frog:\",\":frowning:\",\":frowning_face:\",\":frowning_man:\",\":frowning_person:\",\":frowning_woman:\",\":fu:\",\":middle_finger:\",\":fuelpump:\",\":full_moon:\",\":full_moon_with_face:\",\":funeral_urn:\",\":gabon:\",\":gambia:\",\":game_die:\",\":garlic:\",\":gb:\",\":uk:\",\":gear:\",\":gem:\",\":gemini:\",\":genie:\",\":genie_man:\",\":genie_woman:\",\":georgia:\",\":ghana:\",\":ghost:\",\":gibraltar:\",\":gift:\",\":gift_heart:\",\":ginger_root:\",\":giraffe:\",\":girl:\",\":globe_with_meridians:\",\":gloves:\",\":goal_net:\",\":goat:\",\":goggles:\",\":golf:\",\":golfing:\",\":golfing_man:\",\":golfing_woman:\",\":goose:\",\":gorilla:\",\":grapes:\",\":greece:\",\":green_apple:\",\":green_book:\",\":green_circle:\",\":green_heart:\",\":green_salad:\",\":green_square:\",\":greenland:\",\":grenada:\",\":grey_exclamation:\",\":grey_heart:\",\":grey_question:\",\":grimacing:\",\":grin:\",\":grinning:\",\":guadeloupe:\",\":guam:\",\":guard:\",\":guardsman:\",\":guardswoman:\",\":guatemala:\",\":guernsey:\",\":guide_dog:\",\":guinea:\",\":guinea_bissau:\",\":guitar:\",\":gun:\",\":guyana:\",\":hair_pick:\",\":haircut:\",\":haircut_man:\",\":haircut_woman:\",\":haiti:\",\":hamburger:\",\":hammer:\",\":hammer_and_pick:\",\":hammer_and_wrench:\",\":hamsa:\",\":hamster:\",\":hand:\",\":raised_hand:\",\":hand_over_mouth:\",\":hand_with_index_finger_and_thumb_crossed:\",\":handbag:\",\":handball_person:\",\":handshake:\",\":hankey:\",\":poop:\",\":shit:\",\":hash:\",\":hatched_chick:\",\":hatching_chick:\",\":headphones:\",\":headstone:\",\":health_worker:\",\":hear_no_evil:\",\":heard_mcdonald_islands:\",\":heart:\",\":heart_decoration:\",\":heart_eyes:\",\":heart_eyes_cat:\",\":heart_hands:\",\":heart_on_fire:\",\":heartbeat:\",\":heartpulse:\",\":hearts:\",\":heavy_check_mark:\",\":heavy_division_sign:\",\":heavy_dollar_sign:\",\":heavy_equals_sign:\",\":heavy_heart_exclamation:\",\":heavy_minus_sign:\",\":heavy_multiplication_x:\",\":heavy_plus_sign:\",\":hedgehog:\",\":helicopter:\",\":herb:\",\":hibiscus:\",\":high_brightness:\",\":high_heel:\",\":hiking_boot:\",\":hindu_temple:\",\":hippopotamus:\",\":hocho:\",\":knife:\",\":hole:\",\":honduras:\",\":honey_pot:\",\":hong_kong:\",\":hook:\",\":horse:\",\":horse_racing:\",\":hospital:\",\":hot_face:\",\":hot_pepper:\",\":hotdog:\",\":hotel:\",\":hotsprings:\",\":hourglass:\",\":hourglass_flowing_sand:\",\":house:\",\":house_with_garden:\",\":houses:\",\":hugs:\",\":hungary:\",\":hushed:\",\":hut:\",\":hyacinth:\",\":ice_cream:\",\":ice_cube:\",\":ice_hockey:\",\":ice_skate:\",\":icecream:\",\":iceland:\",\":id:\",\":identification_card:\",\":ideograph_advantage:\",\":imp:\",\":inbox_tray:\",\":incoming_envelope:\",\":index_pointing_at_the_viewer:\",\":india:\",\":indonesia:\",\":infinity:\",\":information_desk_person:\",\":tipping_hand_person:\",\":information_source:\",\":innocent:\",\":interrobang:\",\":iphone:\",\":iran:\",\":iraq:\",\":ireland:\",\":isle_of_man:\",\":israel:\",\":it:\",\":izakaya_lantern:\",\":lantern:\",\":jack_o_lantern:\",\":jamaica:\",\":japan:\",\":japanese_castle:\",\":japanese_goblin:\",\":japanese_ogre:\",\":jar:\",\":jeans:\",\":jellyfish:\",\":jersey:\",\":jigsaw:\",\":jordan:\",\":joy:\",\":joy_cat:\",\":joystick:\",\":jp:\",\":judge:\",\":juggling_person:\",\":kaaba:\",\":kangaroo:\",\":kazakhstan:\",\":kenya:\",\":key:\",\":keyboard:\",\":keycap_ten:\",\":khanda:\",\":kick_scooter:\",\":kimono:\",\":kiribati:\",\":kiss:\",\":kissing:\",\":kissing_cat:\",\":kissing_closed_eyes:\",\":kissing_heart:\",\":kissing_smiling_eyes:\",\":kite:\",\":kiwi_fruit:\",\":kneeling_man:\",\":kneeling_person:\",\":kneeling_woman:\",\":knot:\",\":koala:\",\":koko:\",\":kosovo:\",\":kr:\",\":kuwait:\",\":kyrgyzstan:\",\":lab_coat:\",\":label:\",\":lacrosse:\",\":ladder:\",\":lady_beetle:\",\":laos:\",\":large_blue_circle:\",\":large_blue_diamond:\",\":large_orange_diamond:\",\":last_quarter_moon:\",\":last_quarter_moon_with_face:\",\":latin_cross:\",\":latvia:\",\":laughing:\",\":satisfied:\",\":leafy_green:\",\":leaves:\",\":lebanon:\",\":ledger:\",\":left_luggage:\",\":left_right_arrow:\",\":left_speech_bubble:\",\":leftwards_arrow_with_hook:\",\":leftwards_hand:\",\":leftwards_pushing_hand:\",\":leg:\",\":lemon:\",\":leo:\",\":leopard:\",\":lesotho:\",\":level_slider:\",\":liberia:\",\":libra:\",\":libya:\",\":liechtenstein:\",\":light_blue_heart:\",\":light_rail:\",\":link:\",\":lion:\",\":lips:\",\":lipstick:\",\":lithuania:\",\":lizard:\",\":llama:\",\":lobster:\",\":lock:\",\":lock_with_ink_pen:\",\":lollipop:\",\":long_drum:\",\":loop:\",\":lotion_bottle:\",\":lotus:\",\":lotus_position:\",\":lotus_position_man:\",\":lotus_position_woman:\",\":loud_sound:\",\":loudspeaker:\",\":love_hotel:\",\":love_letter:\",\":love_you_gesture:\",\":low_battery:\",\":low_brightness:\",\":luggage:\",\":lungs:\",\":luxembourg:\",\":lying_face:\",\":m:\",\":macau:\",\":macedonia:\",\":madagascar:\",\":mag:\",\":mag_right:\",\":mage:\",\":mage_man:\",\":mage_woman:\",\":magic_wand:\",\":magnet:\",\":mahjong:\",\":mailbox:\",\":mailbox_closed:\",\":mailbox_with_mail:\",\":mailbox_with_no_mail:\",\":malawi:\",\":malaysia:\",\":maldives:\",\":male_detective:\",\":male_sign:\",\":mali:\",\":malta:\",\":mammoth:\",\":man:\",\":man_artist:\",\":man_astronaut:\",\":man_beard:\",\":man_cartwheeling:\",\":man_cook:\",\":man_dancing:\",\":man_facepalming:\",\":man_factory_worker:\",\":man_farmer:\",\":man_feeding_baby:\",\":man_firefighter:\",\":man_health_worker:\",\":man_in_manual_wheelchair:\",\":man_in_motorized_wheelchair:\",\":man_in_tuxedo:\",\":man_judge:\",\":man_juggling:\",\":man_mechanic:\",\":man_office_worker:\",\":man_pilot:\",\":man_playing_handball:\",\":man_playing_water_polo:\",\":man_scientist:\",\":man_shrugging:\",\":man_singer:\",\":man_student:\",\":man_teacher:\",\":man_technologist:\",\":man_with_gua_pi_mao:\",\":man_with_probing_cane:\",\":man_with_turban:\",\":man_with_veil:\",\":mandarin:\",\":orange:\",\":tangerine:\",\":mango:\",\":mans_shoe:\",\":shoe:\",\":mantelpiece_clock:\",\":manual_wheelchair:\",\":maple_leaf:\",\":maracas:\",\":marshall_islands:\",\":martial_arts_uniform:\",\":martinique:\",\":mask:\",\":massage:\",\":massage_man:\",\":massage_woman:\",\":mate:\",\":mauritania:\",\":mauritius:\",\":mayotte:\",\":meat_on_bone:\",\":mechanic:\",\":mechanical_arm:\",\":mechanical_leg:\",\":medal_military:\",\":medal_sports:\",\":medical_symbol:\",\":mega:\",\":melon:\",\":melting_face:\",\":memo:\",\":pencil:\",\":men_wrestling:\",\":mending_heart:\",\":menorah:\",\":mens:\",\":mermaid:\",\":merman:\",\":merperson:\",\":metal:\",\":metro:\",\":mexico:\",\":microbe:\",\":micronesia:\",\":microphone:\",\":microscope:\",\":military_helmet:\",\":milk_glass:\",\":milky_way:\",\":minibus:\",\":minidisc:\",\":mirror:\",\":mirror_ball:\",\":mobile_phone_off:\",\":moldova:\",\":monaco:\",\":money_mouth_face:\",\":money_with_wings:\",\":moneybag:\",\":mongolia:\",\":monkey:\",\":monkey_face:\",\":monocle_face:\",\":monorail:\",\":montenegro:\",\":montserrat:\",\":moon:\",\":waxing_gibbous_moon:\",\":moon_cake:\",\":moose:\",\":morocco:\",\":mortar_board:\",\":mosque:\",\":mosquito:\",\":motor_boat:\",\":motor_scooter:\",\":motorcycle:\",\":motorized_wheelchair:\",\":motorway:\",\":mount_fuji:\",\":mountain:\",\":mountain_bicyclist:\",\":mountain_biking_man:\",\":mountain_biking_woman:\",\":mountain_cableway:\",\":mountain_railway:\",\":mountain_snow:\",\":mouse:\",\":mouse2:\",\":mouse_trap:\",\":movie_camera:\",\":moyai:\",\":mozambique:\",\":mrs_claus:\",\":muscle:\",\":mushroom:\",\":musical_keyboard:\",\":musical_note:\",\":musical_score:\",\":mute:\",\":mx_claus:\",\":myanmar:\",\":nail_care:\",\":name_badge:\",\":namibia:\",\":national_park:\",\":nauru:\",\":nauseated_face:\",\":nazar_amulet:\",\":necktie:\",\":negative_squared_cross_mark:\",\":nepal:\",\":nerd_face:\",\":nest_with_eggs:\",\":nesting_dolls:\",\":netherlands:\",\":neutral_face:\",\":new:\",\":new_caledonia:\",\":new_moon:\",\":new_moon_with_face:\",\":new_zealand:\",\":newspaper:\",\":newspaper_roll:\",\":next_track_button:\",\":ng:\",\":ng_man:\",\":no_good_man:\",\":ng_woman:\",\":no_good_woman:\",\":nicaragua:\",\":niger:\",\":nigeria:\",\":night_with_stars:\",\":nine:\",\":ninja:\",\":niue:\",\":no_bell:\",\":no_bicycles:\",\":no_entry:\",\":no_entry_sign:\",\":no_good:\",\":no_mobile_phones:\",\":no_mouth:\",\":no_pedestrians:\",\":no_smoking:\",\":non-potable_water:\",\":norfolk_island:\",\":north_korea:\",\":northern_mariana_islands:\",\":norway:\",\":nose:\",\":notebook:\",\":notebook_with_decorative_cover:\",\":notes:\",\":nut_and_bolt:\",\":o:\",\":o2:\",\":ocean:\",\":octopus:\",\":oden:\",\":office:\",\":office_worker:\",\":oil_drum:\",\":ok:\",\":ok_hand:\",\":ok_man:\",\":ok_person:\",\":ok_woman:\",\":old_key:\",\":older_adult:\",\":older_man:\",\":older_woman:\",\":olive:\",\":om:\",\":oman:\",\":on:\",\":oncoming_automobile:\",\":oncoming_bus:\",\":oncoming_police_car:\",\":oncoming_taxi:\",\":one:\",\":one_piece_swimsuit:\",\":onion:\",\":open_file_folder:\",\":open_hands:\",\":open_mouth:\",\":open_umbrella:\",\":ophiuchus:\",\":orange_book:\",\":orange_circle:\",\":orange_heart:\",\":orange_square:\",\":orangutan:\",\":orthodox_cross:\",\":otter:\",\":outbox_tray:\",\":owl:\",\":ox:\",\":oyster:\",\":package:\",\":page_facing_up:\",\":page_with_curl:\",\":pager:\",\":paintbrush:\",\":pakistan:\",\":palau:\",\":palestinian_territories:\",\":palm_down_hand:\",\":palm_tree:\",\":palm_up_hand:\",\":palms_up_together:\",\":panama:\",\":pancakes:\",\":panda_face:\",\":paperclip:\",\":paperclips:\",\":papua_new_guinea:\",\":parachute:\",\":paraguay:\",\":parasol_on_ground:\",\":parking:\",\":parrot:\",\":part_alternation_mark:\",\":partly_sunny:\",\":partying_face:\",\":passenger_ship:\",\":passport_control:\",\":pause_button:\",\":pea_pod:\",\":peace_symbol:\",\":peach:\",\":peacock:\",\":peanuts:\",\":pear:\",\":pen:\",\":pencil2:\",\":penguin:\",\":pensive:\",\":people_holding_hands:\",\":people_hugging:\",\":performing_arts:\",\":persevere:\",\":person_bald:\",\":person_curly_hair:\",\":person_feeding_baby:\",\":person_fencing:\",\":person_in_manual_wheelchair:\",\":person_in_motorized_wheelchair:\",\":person_in_tuxedo:\",\":person_red_hair:\",\":person_white_hair:\",\":person_with_crown:\",\":person_with_probing_cane:\",\":person_with_turban:\",\":person_with_veil:\",\":peru:\",\":petri_dish:\",\":philippines:\",\":phone:\",\":telephone:\",\":pick:\",\":pickup_truck:\",\":pie:\",\":pig:\",\":pig2:\",\":pig_nose:\",\":pill:\",\":pilot:\",\":pinata:\",\":pinched_fingers:\",\":pinching_hand:\",\":pineapple:\",\":ping_pong:\",\":pink_heart:\",\":pirate_flag:\",\":pisces:\",\":pitcairn_islands:\",\":pizza:\",\":placard:\",\":place_of_worship:\",\":plate_with_cutlery:\",\":play_or_pause_button:\",\":playground_slide:\",\":pleading_face:\",\":plunger:\",\":point_down:\",\":point_left:\",\":point_right:\",\":point_up:\",\":point_up_2:\",\":poland:\",\":polar_bear:\",\":police_car:\",\":policeman:\",\":policewoman:\",\":poodle:\",\":popcorn:\",\":portugal:\",\":post_office:\",\":postal_horn:\",\":postbox:\",\":potable_water:\",\":potato:\",\":potted_plant:\",\":pouch:\",\":poultry_leg:\",\":pound:\",\":pouring_liquid:\",\":pout:\",\":rage:\",\":pouting_cat:\",\":pouting_face:\",\":pouting_man:\",\":pouting_woman:\",\":pray:\",\":prayer_beads:\",\":pregnant_man:\",\":pregnant_person:\",\":pregnant_woman:\",\":pretzel:\",\":previous_track_button:\",\":prince:\",\":princess:\",\":printer:\",\":probing_cane:\",\":puerto_rico:\",\":purple_circle:\",\":purple_heart:\",\":purple_square:\",\":purse:\",\":pushpin:\",\":put_litter_in_its_place:\",\":qatar:\",\":question:\",\":rabbit:\",\":rabbit2:\",\":raccoon:\",\":racehorse:\",\":racing_car:\",\":radio:\",\":radio_button:\",\":radioactive:\",\":railway_car:\",\":railway_track:\",\":rainbow:\",\":rainbow_flag:\",\":raised_back_of_hand:\",\":raised_eyebrow:\",\":raised_hand_with_fingers_splayed:\",\":raised_hands:\",\":raising_hand:\",\":raising_hand_man:\",\":raising_hand_woman:\",\":ram:\",\":ramen:\",\":rat:\",\":razor:\",\":receipt:\",\":record_button:\",\":recycle:\",\":red_circle:\",\":red_envelope:\",\":red_haired_man:\",\":red_haired_woman:\",\":red_square:\",\":registered:\",\":relaxed:\",\":relieved:\",\":reminder_ribbon:\",\":repeat:\",\":repeat_one:\",\":rescue_worker_helmet:\",\":restroom:\",\":reunion:\",\":revolving_hearts:\",\":rewind:\",\":rhinoceros:\",\":ribbon:\",\":rice:\",\":rice_ball:\",\":rice_cracker:\",\":rice_scene:\",\":right_anger_bubble:\",\":rightwards_hand:\",\":rightwards_pushing_hand:\",\":ring:\",\":ring_buoy:\",\":ringed_planet:\",\":robot:\",\":rock:\",\":rocket:\",\":rofl:\",\":roll_eyes:\",\":roll_of_paper:\",\":roller_coaster:\",\":roller_skate:\",\":romania:\",\":rooster:\",\":rose:\",\":rosette:\",\":rotating_light:\",\":round_pushpin:\",\":rowboat:\",\":rowing_man:\",\":rowing_woman:\",\":ru:\",\":rugby_football:\",\":runner:\",\":running:\",\":running_man:\",\":running_shirt_with_sash:\",\":running_woman:\",\":rwanda:\",\":sa:\",\":safety_pin:\",\":safety_vest:\",\":sagittarius:\",\":sake:\",\":salt:\",\":saluting_face:\",\":samoa:\",\":san_marino:\",\":sandal:\",\":sandwich:\",\":santa:\",\":sao_tome_principe:\",\":sari:\",\":sassy_man:\",\":tipping_hand_man:\",\":sassy_woman:\",\":tipping_hand_woman:\",\":satellite:\",\":saudi_arabia:\",\":sauna_man:\",\":sauna_person:\",\":sauna_woman:\",\":sauropod:\",\":saxophone:\",\":scarf:\",\":school:\",\":school_satchel:\",\":scientist:\",\":scissors:\",\":scorpion:\",\":scorpius:\",\":scotland:\",\":scream:\",\":scream_cat:\",\":screwdriver:\",\":scroll:\",\":seal:\",\":seat:\",\":secret:\",\":see_no_evil:\",\":seedling:\",\":selfie:\",\":senegal:\",\":serbia:\",\":service_dog:\",\":seven:\",\":sewing_needle:\",\":seychelles:\",\":shaking_face:\",\":shallow_pan_of_food:\",\":shamrock:\",\":shark:\",\":shaved_ice:\",\":sheep:\",\":shell:\",\":shield:\",\":shinto_shrine:\",\":ship:\",\":shirt:\",\":tshirt:\",\":shopping:\",\":shopping_cart:\",\":shorts:\",\":shower:\",\":shrimp:\",\":shrug:\",\":shushing_face:\",\":sierra_leone:\",\":signal_strength:\",\":singapore:\",\":singer:\",\":sint_maarten:\",\":six:\",\":six_pointed_star:\",\":skateboard:\",\":ski:\",\":skier:\",\":skull:\",\":skull_and_crossbones:\",\":skunk:\",\":sled:\",\":sleeping:\",\":sleeping_bed:\",\":sleepy:\",\":slightly_frowning_face:\",\":slightly_smiling_face:\",\":slot_machine:\",\":sloth:\",\":slovakia:\",\":slovenia:\",\":small_airplane:\",\":small_blue_diamond:\",\":small_orange_diamond:\",\":small_red_triangle:\",\":small_red_triangle_down:\",\":smile:\",\":smile_cat:\",\":smiley:\",\":smiley_cat:\",\":smiling_face_with_tear:\",\":smiling_face_with_three_hearts:\",\":smiling_imp:\",\":smirk:\",\":smirk_cat:\",\":smoking:\",\":snail:\",\":snake:\",\":sneezing_face:\",\":snowboarder:\",\":snowflake:\",\":snowman:\",\":snowman_with_snow:\",\":soap:\",\":sob:\",\":soccer:\",\":socks:\",\":softball:\",\":solomon_islands:\",\":somalia:\",\":soon:\",\":sos:\",\":sound:\",\":south_africa:\",\":south_georgia_south_sandwich_islands:\",\":south_sudan:\",\":space_invader:\",\":spades:\",\":spaghetti:\",\":sparkle:\",\":sparkler:\",\":sparkles:\",\":sparkling_heart:\",\":speak_no_evil:\",\":speaker:\",\":speaking_head:\",\":speech_balloon:\",\":speedboat:\",\":spider:\",\":spider_web:\",\":spiral_calendar:\",\":spiral_notepad:\",\":sponge:\",\":spoon:\",\":squid:\",\":sri_lanka:\",\":st_barthelemy:\",\":st_helena:\",\":st_kitts_nevis:\",\":st_lucia:\",\":st_martin:\",\":st_pierre_miquelon:\",\":st_vincent_grenadines:\",\":stadium:\",\":standing_man:\",\":standing_person:\",\":standing_woman:\",\":star:\",\":star2:\",\":star_and_crescent:\",\":star_of_david:\",\":star_struck:\",\":stars:\",\":station:\",\":statue_of_liberty:\",\":steam_locomotive:\",\":stethoscope:\",\":stew:\",\":stop_button:\",\":stop_sign:\",\":stopwatch:\",\":straight_ruler:\",\":strawberry:\",\":stuck_out_tongue:\",\":stuck_out_tongue_closed_eyes:\",\":stuck_out_tongue_winking_eye:\",\":student:\",\":studio_microphone:\",\":stuffed_flatbread:\",\":sudan:\",\":sun_behind_large_cloud:\",\":sun_behind_rain_cloud:\",\":sun_behind_small_cloud:\",\":sun_with_face:\",\":sunflower:\",\":sunglasses:\",\":sunny:\",\":sunrise:\",\":sunrise_over_mountains:\",\":superhero:\",\":superhero_man:\",\":superhero_woman:\",\":supervillain:\",\":supervillain_man:\",\":supervillain_woman:\",\":surfer:\",\":surfing_man:\",\":surfing_woman:\",\":suriname:\",\":sushi:\",\":suspension_railway:\",\":svalbard_jan_mayen:\",\":swan:\",\":swaziland:\",\":sweat:\",\":sweat_drops:\",\":sweat_smile:\",\":sweden:\",\":sweet_potato:\",\":swim_brief:\",\":swimmer:\",\":swimming_man:\",\":swimming_woman:\",\":switzerland:\",\":symbols:\",\":synagogue:\",\":syria:\",\":syringe:\",\":t-rex:\",\":taco:\",\":tada:\",\":taiwan:\",\":tajikistan:\",\":takeout_box:\",\":tamale:\",\":tanabata_tree:\",\":tanzania:\",\":taurus:\",\":taxi:\",\":tea:\",\":teacher:\",\":teapot:\",\":technologist:\",\":teddy_bear:\",\":telephone_receiver:\",\":telescope:\",\":tennis:\",\":tent:\",\":test_tube:\",\":thailand:\",\":thermometer:\",\":thinking:\",\":thong_sandal:\",\":thought_balloon:\",\":thread:\",\":three:\",\":ticket:\",\":tickets:\",\":tiger:\",\":tiger2:\",\":timer_clock:\",\":timor_leste:\",\":tired_face:\",\":tm:\",\":togo:\",\":toilet:\",\":tokelau:\",\":tokyo_tower:\",\":tomato:\",\":tonga:\",\":tongue:\",\":toolbox:\",\":tooth:\",\":toothbrush:\",\":top:\",\":tophat:\",\":tornado:\",\":tr:\",\":trackball:\",\":tractor:\",\":traffic_light:\",\":train:\",\":train2:\",\":tram:\",\":transgender_flag:\",\":transgender_symbol:\",\":triangular_flag_on_post:\",\":triangular_ruler:\",\":trident:\",\":trinidad_tobago:\",\":tristan_da_cunha:\",\":triumph:\",\":troll:\",\":trolleybus:\",\":trophy:\",\":tropical_drink:\",\":tropical_fish:\",\":truck:\",\":trumpet:\",\":tulip:\",\":tumbler_glass:\",\":tunisia:\",\":turkey:\",\":turkmenistan:\",\":turks_caicos_islands:\",\":turtle:\",\":tuvalu:\",\":tv:\",\":twisted_rightwards_arrows:\",\":two:\",\":two_hearts:\",\":two_men_holding_hands:\",\":two_women_holding_hands:\",\":u5272:\",\":u5408:\",\":u55b6:\",\":u6307:\",\":u6708:\",\":u6709:\",\":u6e80:\",\":u7121:\",\":u7533:\",\":u7981:\",\":u7a7a:\",\":uganda:\",\":ukraine:\",\":umbrella:\",\":unamused:\",\":underage:\",\":unicorn:\",\":united_arab_emirates:\",\":united_nations:\",\":unlock:\",\":up:\",\":upside_down_face:\",\":uruguay:\",\":us:\",\":us_outlying_islands:\",\":us_virgin_islands:\",\":uzbekistan:\",\":v:\",\":vampire:\",\":vampire_man:\",\":vampire_woman:\",\":vanuatu:\",\":vatican_city:\",\":venezuela:\",\":vertical_traffic_light:\",\":vhs:\",\":vibration_mode:\",\":video_camera:\",\":video_game:\",\":vietnam:\",\":violin:\",\":virgo:\",\":volcano:\",\":volleyball:\",\":vomiting_face:\",\":vs:\",\":vulcan_salute:\",\":waffle:\",\":wales:\",\":walking:\",\":walking_man:\",\":walking_woman:\",\":wallis_futuna:\",\":waning_crescent_moon:\",\":waning_gibbous_moon:\",\":warning:\",\":wastebasket:\",\":watch:\",\":water_buffalo:\",\":water_polo:\",\":watermelon:\",\":wave:\",\":wavy_dash:\",\":waxing_crescent_moon:\",\":wc:\",\":weary:\",\":wedding:\",\":weight_lifting:\",\":weight_lifting_man:\",\":weight_lifting_woman:\",\":western_sahara:\",\":whale:\",\":whale2:\",\":wheel:\",\":wheel_of_dharma:\",\":wheelchair:\",\":white_check_mark:\",\":white_circle:\",\":white_flag:\",\":white_flower:\",\":white_haired_man:\",\":white_haired_woman:\",\":white_heart:\",\":white_large_square:\",\":white_medium_small_square:\",\":white_medium_square:\",\":white_small_square:\",\":white_square_button:\",\":wilted_flower:\",\":wind_chime:\",\":wind_face:\",\":window:\",\":wine_glass:\",\":wing:\",\":wink:\",\":wireless:\",\":wolf:\",\":woman:\",\":woman_artist:\",\":woman_astronaut:\",\":woman_beard:\",\":woman_cartwheeling:\",\":woman_cook:\",\":woman_facepalming:\",\":woman_factory_worker:\",\":woman_farmer:\",\":woman_feeding_baby:\",\":woman_firefighter:\",\":woman_health_worker:\",\":woman_in_manual_wheelchair:\",\":woman_in_motorized_wheelchair:\",\":woman_in_tuxedo:\",\":woman_judge:\",\":woman_juggling:\",\":woman_mechanic:\",\":woman_office_worker:\",\":woman_pilot:\",\":woman_playing_handball:\",\":woman_playing_water_polo:\",\":woman_scientist:\",\":woman_shrugging:\",\":woman_singer:\",\":woman_student:\",\":woman_teacher:\",\":woman_technologist:\",\":woman_with_headscarf:\",\":woman_with_probing_cane:\",\":woman_with_turban:\",\":womans_clothes:\",\":womans_hat:\",\":women_wrestling:\",\":womens:\",\":wood:\",\":woozy_face:\",\":world_map:\",\":worm:\",\":worried:\",\":wrench:\",\":wrestling:\",\":writing_hand:\",\":x:\",\":x_ray:\",\":yarn:\",\":yawning_face:\",\":yellow_circle:\",\":yellow_heart:\",\":yellow_square:\",\":yemen:\",\":yen:\",\":yin_yang:\",\":yo_yo:\",\":yum:\",\":zambia:\",\":zany_face:\",\":zap:\",\":zebra:\",\":zero:\",\":zimbabwe:\",\":zipper_mouth_face:\",\":zombie:\",\":zombie_man:\",\":zombie_woman:\",\":zzz:\"]"
    )

    val lastEnteredKeywordListInGitHubString =
        "[" + currentKeywordListGitHub.joinToString(", ") { "\"" + it + "\"" } + "]"
    if (fetchedGhList != lastEnteredKeywordListInGitHubString) {
        error("The GitHub emoji list changed. Please enter the string in a GitHub markdown editor, and copy the preview string into the variable emojisListGH and set the keyword string to the new value of currentKeywordListGitHub\n$fetchedGhList")
    }

    val emojisListGitHub: List<String> = jacksonObjectMapper().readValue(
        "[\"\uD83D\uDC4D\",\"\uD83D\uDC4D\",\"\uD83D\uDC4E\",\"\uD83D\uDC4E\",\"\uD83D\uDCAF\",\"\uD83D\uDD22\",\"\uD83E\uDD47\",\"\uD83E\uDD48\",\"\uD83E\uDD49\",\"\uD83C\uDFB1\",\"\uD83C\uDD70\uFE0F\",\"\uD83C\uDD8E\",\"\uD83E\uDDEE\",\"\uD83D\uDD24\",\"\uD83D\uDD21\",\"\uD83C\uDE51\",\"\uD83E\uDE97\",\"\uD83E\uDE79\",\"\uD83E\uDDD1\",\"\uD83D\uDEA1\",\"\uD83C\uDDE6\uD83C\uDDEB\",\"✈\uFE0F\",\"\uD83C\uDDE6\uD83C\uDDFD\",\"⏰\",\"\uD83C\uDDE6\uD83C\uDDF1\",\"⚗\uFE0F\",\"\uD83C\uDDE9\uD83C\uDDFF\",\"\uD83D\uDC7D\",\"\uD83D\uDE91\",\"\uD83C\uDDE6\uD83C\uDDF8\",\"\uD83C\uDFFA\",\"\uD83E\uDEC0\",\"⚓\",\"\uD83C\uDDE6\uD83C\uDDE9\",\"\uD83D\uDC7C\",\"\uD83D\uDCA2\",\"\uD83C\uDDE6\uD83C\uDDF4\",\"\uD83D\uDE20\",\"\uD83C\uDDE6\uD83C\uDDEE\",\"\uD83D\uDE27\",\"\uD83D\uDC1C\",\"\uD83C\uDDE6\uD83C\uDDF6\",\"\uD83C\uDDE6\uD83C\uDDEC\",\"\uD83C\uDF4E\",\"♒\",\"\uD83C\uDDE6\uD83C\uDDF7\",\"♈\",\"\uD83C\uDDE6\uD83C\uDDF2\",\"◀\uFE0F\",\"⏬\",\"⏫\",\"⬇\uFE0F\",\"\uD83D\uDD3D\",\"▶\uFE0F\",\"⤵\uFE0F\",\"⤴\uFE0F\",\"⬅\uFE0F\",\"↙\uFE0F\",\"↘\uFE0F\",\"➡\uFE0F\",\"↪\uFE0F\",\"⬆\uFE0F\",\"↕\uFE0F\",\"\uD83D\uDD3C\",\"↖\uFE0F\",\"↗\uFE0F\",\"\uD83D\uDD03\",\"\uD83D\uDD04\",\"\uD83C\uDFA8\",\"\uD83D\uDE9B\",\"\uD83D\uDEF0\uFE0F\",\"\uD83E\uDDD1\u200D\uD83C\uDFA8\",\"\uD83C\uDDE6\uD83C\uDDFC\",\"\uD83C\uDDE6\uD83C\uDDE8\",\"*\uFE0F⃣\",\"\uD83D\uDE32\",\"\uD83E\uDDD1\u200D\uD83D\uDE80\",\"\uD83D\uDC5F\",\"\uD83C\uDFE7\",\"⚛\uFE0F\",\"\uD83C\uDDE6\uD83C\uDDFA\",\"\uD83C\uDDE6\uD83C\uDDF9\",\"\uD83D\uDEFA\",\"\uD83E\uDD51\",\"\uD83E\uDE93\",\"\uD83C\uDDE6\uD83C\uDDFF\",\"\uD83C\uDD71\uFE0F\",\"\uD83D\uDC76\",\"\uD83C\uDF7C\",\"\uD83D\uDC24\",\"\uD83D\uDEBC\",\"\uD83D\uDD19\",\"\uD83E\uDD53\",\"\uD83E\uDDA1\",\"\uD83C\uDFF8\",\"\uD83E\uDD6F\",\"\uD83D\uDEC4\",\"\uD83E\uDD56\",\"\uD83C\uDDE7\uD83C\uDDF8\",\"\uD83C\uDDE7\uD83C\uDDED\",\"⚖\uFE0F\",\"\uD83D\uDC68\u200D\uD83E\uDDB2\",\"\uD83D\uDC69\u200D\uD83E\uDDB2\",\"\uD83E\uDE70\",\"\uD83C\uDF88\",\"\uD83D\uDDF3\uFE0F\",\"☑\uFE0F\",\"\uD83C\uDF8D\",\"\uD83C\uDF4C\",\"‼\uFE0F\",\"\uD83C\uDDE7\uD83C\uDDE9\",\"\uD83E\uDE95\",\"\uD83C\uDFE6\",\"\uD83D\uDCCA\",\"\uD83C\uDDE7\uD83C\uDDE7\",\"\uD83D\uDC88\",\"⚾\",\"\uD83E\uDDFA\",\"\uD83C\uDFC0\",\"⛹\uFE0F\u200D♂\uFE0F\",\"⛹\uFE0F\u200D♂\uFE0F\",\"⛹\uFE0F\u200D♀\uFE0F\",\"⛹\uFE0F\u200D♀\uFE0F\",\"\uD83E\uDD87\",\"\uD83D\uDEC0\",\"\uD83D\uDEC1\",\"\uD83D\uDD0B\",\"\uD83C\uDFD6\uFE0F\",\"\uD83E\uDED8\",\"\uD83D\uDC3B\",\"\uD83E\uDDD4\",\"\uD83E\uDDAB\",\"\uD83D\uDECF\uFE0F\",\"\uD83D\uDC1D\",\"\uD83D\uDC1D\",\"\uD83C\uDF7A\",\"\uD83C\uDF7B\",\"\uD83E\uDEB2\",\"\uD83D\uDD30\",\"\uD83C\uDDE7\uD83C\uDDFE\",\"\uD83C\uDDE7\uD83C\uDDEA\",\"\uD83C\uDDE7\uD83C\uDDFF\",\"\uD83D\uDD14\",\"\uD83E\uDED1\",\"\uD83D\uDECE\uFE0F\",\"\uD83C\uDDE7\uD83C\uDDEF\",\"\uD83C\uDF71\",\"\uD83C\uDDE7\uD83C\uDDF2\",\"\uD83E\uDDC3\",\"\uD83C\uDDE7\uD83C\uDDF9\",\"\uD83D\uDEB4\",\"\uD83D\uDEB2\",\"\uD83D\uDEB4\u200D♂\uFE0F\",\"\uD83D\uDEB4\u200D♀\uFE0F\",\"\uD83D\uDC59\",\"\uD83E\uDDE2\",\"☣\uFE0F\",\"\uD83D\uDC26\",\"\uD83C\uDF82\",\"\uD83E\uDDAC\",\"\uD83E\uDEE6\",\"\uD83D\uDC26\u200D⬛\",\"\uD83D\uDC08\u200D⬛\",\"⚫\",\"\uD83C\uDFF4\",\"\uD83D\uDDA4\",\"\uD83C\uDCCF\",\"⬛\",\"◾\",\"◼\uFE0F\",\"✒\uFE0F\",\"▪\uFE0F\",\"\uD83D\uDD32\",\"\uD83D\uDC71\u200D♂\uFE0F\",\"\uD83D\uDC71\",\"\uD83D\uDC71\u200D♀\uFE0F\",\"\uD83D\uDC71\u200D♀\uFE0F\",\"\uD83C\uDF3C\",\"\uD83D\uDC21\",\"\uD83D\uDCD8\",\"\uD83D\uDE99\",\"\uD83D\uDC99\",\"\uD83D\uDFE6\",\"\uD83E\uDED0\",\"\uD83D\uDE0A\",\"\uD83D\uDC17\",\"⛵\",\"⛵\",\"\uD83C\uDDE7\uD83C\uDDF4\",\"\uD83D\uDCA3\",\"\uD83E\uDDB4\",\"\uD83D\uDCD6\",\"\uD83D\uDCD6\",\"\uD83D\uDD16\",\"\uD83D\uDCD1\",\"\uD83D\uDCDA\",\"\uD83D\uDCA5\",\"\uD83D\uDCA5\",\"\uD83E\uDE83\",\"\uD83D\uDC62\",\"\uD83C\uDDE7\uD83C\uDDE6\",\"\uD83C\uDDE7\uD83C\uDDFC\",\"⛹\uFE0F\",\"\uD83D\uDC90\",\"\uD83C\uDDE7\uD83C\uDDFB\",\"\uD83D\uDE47\",\"\uD83C\uDFF9\",\"\uD83D\uDE47\u200D♂\uFE0F\",\"\uD83D\uDE47\u200D♀\uFE0F\",\"\uD83E\uDD63\",\"\uD83C\uDFB3\",\"\uD83E\uDD4A\",\"\uD83D\uDC66\",\"\uD83E\uDDE0\",\"\uD83C\uDDE7\uD83C\uDDF7\",\"\uD83C\uDF5E\",\"\uD83E\uDD31\",\"\uD83E\uDDF1\",\"\uD83D\uDC70\u200D♀\uFE0F\",\"\uD83D\uDC70\u200D♀\uFE0F\",\"\uD83C\uDF09\",\"\uD83D\uDCBC\",\"\uD83C\uDDEE\uD83C\uDDF4\",\"\uD83C\uDDFB\uD83C\uDDEC\",\"\uD83E\uDD66\",\"\uD83D\uDC94\",\"\uD83E\uDDF9\",\"\uD83D\uDFE4\",\"\uD83E\uDD0E\",\"\uD83D\uDFEB\",\"\uD83C\uDDE7\uD83C\uDDF3\",\"\uD83E\uDDCB\",\"\uD83E\uDEE7\",\"\uD83E\uDEA3\",\"\uD83D\uDC1B\",\"\uD83C\uDFD7\uFE0F\",\"\uD83D\uDCA1\",\"\uD83C\uDDE7\uD83C\uDDEC\",\"\uD83D\uDE85\",\"\uD83D\uDE84\",\"\uD83C\uDDE7\uD83C\uDDEB\",\"\uD83C\uDF2F\",\"\uD83C\uDDE7\uD83C\uDDEE\",\"\uD83D\uDE8C\",\"\uD83D\uDD74\uFE0F\",\"\uD83D\uDE8F\",\"\uD83D\uDC64\",\"\uD83D\uDC65\",\"\uD83E\uDDC8\",\"\uD83E\uDD8B\",\"\uD83C\uDF35\",\"\uD83C\uDF70\",\"\uD83D\uDCC6\",\"\uD83E\uDD19\",\"\uD83D\uDCF2\",\"\uD83C\uDDF0\uD83C\uDDED\",\"\uD83D\uDC2B\",\"\uD83D\uDCF7\",\"\uD83D\uDCF8\",\"\uD83C\uDDE8\uD83C\uDDF2\",\"\uD83C\uDFD5\uFE0F\",\"\uD83C\uDDE8\uD83C\uDDE6\",\"\uD83C\uDDEE\uD83C\uDDE8\",\"♋\",\"\uD83D\uDD6F\uFE0F\",\"\uD83C\uDF6C\",\"\uD83E\uDD6B\",\"\uD83D\uDEF6\",\"\uD83C\uDDE8\uD83C\uDDFB\",\"\uD83D\uDD20\",\"♑\",\"\uD83D\uDE97\",\"\uD83D\uDE97\",\"\uD83D\uDDC3\uFE0F\",\"\uD83D\uDCC7\",\"\uD83D\uDDC2\uFE0F\",\"\uD83C\uDDE7\uD83C\uDDF6\",\"\uD83C\uDFA0\",\"\uD83E\uDE9A\",\"\uD83E\uDD55\",\"\uD83E\uDD38\",\"\uD83D\uDC31\",\"\uD83D\uDC08\",\"\uD83C\uDDF0\uD83C\uDDFE\",\"\uD83D\uDCBF\",\"\uD83C\uDDE8\uD83C\uDDEB\",\"\uD83C\uDDEA\uD83C\uDDE6\",\"\uD83C\uDDF9\uD83C\uDDE9\",\"⛓\uFE0F\",\"\uD83E\uDE91\",\"\uD83C\uDF7E\",\"\uD83D\uDCB9\",\"\uD83D\uDCC9\",\"\uD83D\uDCC8\",\"\uD83C\uDFC1\",\"\uD83E\uDDC0\",\"\uD83C\uDF52\",\"\uD83C\uDF38\",\"♟\uFE0F\",\"\uD83C\uDF30\",\"\uD83D\uDC14\",\"\uD83E\uDDD2\",\"\uD83D\uDEB8\",\"\uD83C\uDDE8\uD83C\uDDF1\",\"\uD83D\uDC3F\uFE0F\",\"\uD83C\uDF6B\",\"\uD83E\uDD62\",\"\uD83C\uDDE8\uD83C\uDDFD\",\"\uD83C\uDF84\",\"⛪\",\"\uD83C\uDFA6\",\"\uD83C\uDFAA\",\"\uD83C\uDF07\",\"\uD83C\uDF06\",\"\uD83C\uDFD9\uFE0F\",\"\uD83C\uDD91\",\"\uD83D\uDDDC\uFE0F\",\"\uD83D\uDC4F\",\"\uD83C\uDFAC\",\"\uD83C\uDFDB\uFE0F\",\"\uD83E\uDDD7\",\"\uD83E\uDDD7\u200D♂\uFE0F\",\"\uD83E\uDDD7\u200D♀\uFE0F\",\"\uD83E\uDD42\",\"\uD83D\uDCCB\",\"\uD83C\uDDE8\uD83C\uDDF5\",\"\uD83D\uDD50\",\"\uD83D\uDD59\",\"\uD83D\uDD65\",\"\uD83D\uDD5A\",\"\uD83D\uDD66\",\"\uD83D\uDD5B\",\"\uD83D\uDD67\",\"\uD83D\uDD5C\",\"\uD83D\uDD51\",\"\uD83D\uDD5D\",\"\uD83D\uDD52\",\"\uD83D\uDD5E\",\"\uD83D\uDD53\",\"\uD83D\uDD5F\",\"\uD83D\uDD54\",\"\uD83D\uDD60\",\"\uD83D\uDD55\",\"\uD83D\uDD61\",\"\uD83D\uDD56\",\"\uD83D\uDD62\",\"\uD83D\uDD57\",\"\uD83D\uDD63\",\"\uD83D\uDD58\",\"\uD83D\uDD64\",\"\uD83D\uDCD5\",\"\uD83D\uDD10\",\"\uD83C\uDF02\",\"☁\uFE0F\",\"\uD83C\uDF29\uFE0F\",\"⛈\uFE0F\",\"\uD83C\uDF27\uFE0F\",\"\uD83C\uDF28\uFE0F\",\"\uD83E\uDD21\",\"♣\uFE0F\",\"\uD83C\uDDE8\uD83C\uDDF3\",\"\uD83E\uDDE5\",\"\uD83E\uDEB3\",\"\uD83C\uDF78\",\"\uD83E\uDD65\",\"\uD83C\uDDE8\uD83C\uDDE8\",\"☕\",\"⚰\uFE0F\",\"\uD83E\uDE99\",\"\uD83E\uDD76\",\"\uD83D\uDE30\",\"\uD83C\uDDE8\uD83C\uDDF4\",\"☄\uFE0F\",\"\uD83C\uDDF0\uD83C\uDDF2\",\"\uD83E\uDDED\",\"\uD83D\uDCBB\",\"\uD83D\uDDB1\uFE0F\",\"\uD83C\uDF8A\",\"\uD83D\uDE16\",\"\uD83D\uDE15\",\"\uD83C\uDDE8\uD83C\uDDEC\",\"\uD83C\uDDE8\uD83C\uDDE9\",\"㊗\uFE0F\",\"\uD83D\uDEA7\",\"\uD83D\uDC77\",\"\uD83D\uDC77\u200D♂\uFE0F\",\"\uD83D\uDC77\u200D♀\uFE0F\",\"\uD83C\uDF9B\uFE0F\",\"\uD83C\uDFEA\",\"\uD83E\uDDD1\u200D\uD83C\uDF73\",\"\uD83C\uDDE8\uD83C\uDDF0\",\"\uD83C\uDF6A\",\"\uD83C\uDD92\",\"\uD83D\uDC6E\",\"\uD83D\uDC6E\",\"©\uFE0F\",\"\uD83E\uDEB8\",\"\uD83C\uDF3D\",\"\uD83C\uDDE8\uD83C\uDDF7\",\"\uD83C\uDDE8\uD83C\uDDEE\",\"\uD83D\uDECB\uFE0F\",\"\uD83D\uDC6B\",\"\uD83D\uDC91\",\"\uD83D\uDC68\u200D❤\uFE0F\u200D\uD83D\uDC68\",\"\uD83D\uDC69\u200D❤\uFE0F\u200D\uD83D\uDC68\",\"\uD83D\uDC69\u200D❤\uFE0F\u200D\uD83D\uDC69\",\"\uD83D\uDC8F\",\"\uD83D\uDC68\u200D❤\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC68\",\"\uD83D\uDC69\u200D❤\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC68\",\"\uD83D\uDC69\u200D❤\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC69\",\"\uD83D\uDC2E\",\"\uD83D\uDC04\",\"\uD83E\uDD20\",\"\uD83E\uDD80\",\"\uD83D\uDD8D\uFE0F\",\"\uD83D\uDCB3\",\"\uD83C\uDF19\",\"\uD83E\uDD97\",\"\uD83C\uDFCF\",\"\uD83C\uDDED\uD83C\uDDF7\",\"\uD83D\uDC0A\",\"\uD83E\uDD50\",\"\uD83E\uDD1E\",\"\uD83C\uDF8C\",\"⚔\uFE0F\",\"\uD83D\uDC51\",\"\uD83E\uDE7C\",\"\uD83D\uDE22\",\"\uD83D\uDE3F\",\"\uD83D\uDD2E\",\"\uD83C\uDDE8\uD83C\uDDFA\",\"\uD83E\uDD52\",\"\uD83E\uDD64\",\"\uD83E\uDDC1\",\"\uD83D\uDC98\",\"\uD83C\uDDE8\uD83C\uDDFC\",\"\uD83E\uDD4C\",\"\uD83D\uDC68\u200D\uD83E\uDDB1\",\"\uD83D\uDC69\u200D\uD83E\uDDB1\",\"➰\",\"\uD83D\uDCB1\",\"\uD83C\uDF5B\",\"\uD83E\uDD2C\",\"\uD83C\uDF6E\",\"\uD83D\uDEC3\",\"\uD83E\uDD69\",\"\uD83C\uDF00\",\"\uD83C\uDDE8\uD83C\uDDFE\",\"\uD83C\uDDE8\uD83C\uDDFF\",\"\uD83D\uDDE1\uFE0F\",\"\uD83D\uDC83\",\"\uD83D\uDC83\",\"\uD83D\uDC6F\",\"\uD83D\uDC6F\u200D♂\uFE0F\",\"\uD83D\uDC6F\u200D♀\uFE0F\",\"\uD83C\uDF61\",\"\uD83D\uDD76\uFE0F\",\"\uD83C\uDFAF\",\"\uD83D\uDCA8\",\"\uD83D\uDCC5\",\"\uD83C\uDDE9\uD83C\uDDEA\",\"\uD83E\uDDCF\u200D♂\uFE0F\",\"\uD83E\uDDCF\",\"\uD83E\uDDCF\u200D♀\uFE0F\",\"\uD83C\uDF33\",\"\uD83E\uDD8C\",\"\uD83C\uDDE9\uD83C\uDDF0\",\"\uD83C\uDFEC\",\"\uD83C\uDFDA\uFE0F\",\"\uD83C\uDFDC\uFE0F\",\"\uD83C\uDFDD\uFE0F\",\"\uD83D\uDDA5\uFE0F\",\"\uD83D\uDD75\uFE0F\",\"\uD83D\uDCA0\",\"♦\uFE0F\",\"\uD83C\uDDE9\uD83C\uDDEC\",\"\uD83D\uDE1E\",\"\uD83D\uDE25\",\"\uD83E\uDD78\",\"\uD83E\uDD3F\",\"\uD83E\uDE94\",\"\uD83D\uDCAB\",\"\uD83D\uDE35\",\"\uD83C\uDDE9\uD83C\uDDEF\",\"\uD83E\uDDEC\",\"\uD83D\uDEAF\",\"\uD83E\uDDA4\",\"\uD83D\uDC36\",\"\uD83D\uDC15\",\"\uD83D\uDCB5\",\"\uD83C\uDF8E\",\"\uD83D\uDC2C\",\"\uD83D\uDC2C\",\"\uD83C\uDDE9\uD83C\uDDF2\",\"\uD83C\uDDE9\uD83C\uDDF4\",\"\uD83E\uDECF\",\"\uD83D\uDEAA\",\"\uD83E\uDEE5\",\"\uD83C\uDF69\",\"\uD83D\uDD4A\uFE0F\",\"\uD83D\uDC09\",\"\uD83D\uDC32\",\"\uD83D\uDC57\",\"\uD83D\uDC2A\",\"\uD83E\uDD24\",\"\uD83E\uDE78\",\"\uD83D\uDCA7\",\"\uD83E\uDD41\",\"\uD83E\uDD86\",\"\uD83E\uDD5F\",\"\uD83D\uDCC0\",\"\uD83D\uDCE7\",\"\uD83D\uDCE7\",\"\uD83E\uDD85\",\"\uD83D\uDC42\",\"\uD83C\uDF3E\",\"\uD83E\uDDBB\",\"\uD83C\uDF0D\",\"\uD83C\uDF0E\",\"\uD83C\uDF0F\",\"\uD83C\uDDEA\uD83C\uDDE8\",\"\uD83E\uDD5A\",\"\uD83C\uDF46\",\"\uD83C\uDDEA\uD83C\uDDEC\",\"8\uFE0F⃣\",\"✴\uFE0F\",\"✳\uFE0F\",\"⏏\uFE0F\",\"\uD83C\uDDF8\uD83C\uDDFB\",\"\uD83D\uDD0C\",\"\uD83D\uDC18\",\"\uD83D\uDED7\",\"\uD83E\uDDDD\",\"\uD83E\uDDDD\u200D♂\uFE0F\",\"\uD83E\uDDDD\u200D♀\uFE0F\",\"\uD83E\uDEB9\",\"\uD83D\uDD1A\",\"\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F\",\"✉\uFE0F\",\"\uD83D\uDCE9\",\"\uD83C\uDDEC\uD83C\uDDF6\",\"\uD83C\uDDEA\uD83C\uDDF7\",\"\uD83C\uDDEA\uD83C\uDDF8\",\"\uD83C\uDDEA\uD83C\uDDEA\",\"\uD83C\uDDEA\uD83C\uDDF9\",\"\uD83C\uDDEA\uD83C\uDDFA\",\"\uD83C\uDDEA\uD83C\uDDFA\",\"\uD83D\uDCB6\",\"\uD83C\uDFF0\",\"\uD83C\uDFE4\",\"\uD83C\uDF32\",\"❗\",\"❗\",\"\uD83E\uDD2F\",\"\uD83D\uDE11\",\"\uD83D\uDC41\uFE0F\",\"\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F\",\"\uD83D\uDC53\",\"\uD83D\uDC40\",\"\uD83D\uDE2E\u200D\uD83D\uDCA8\",\"\uD83E\uDD79\",\"\uD83D\uDE36\u200D\uD83C\uDF2B\uFE0F\",\"\uD83E\uDEE4\",\"\uD83E\uDD15\",\"\uD83E\uDEE2\",\"\uD83E\uDEE3\",\"\uD83D\uDE35\u200D\uD83D\uDCAB\",\"\uD83E\uDD12\",\"\uD83E\uDD26\",\"\uD83D\uDC4A\",\"\uD83D\uDC4A\",\"\uD83D\uDC4A\",\"\uD83C\uDFED\",\"\uD83E\uDDD1\u200D\uD83C\uDFED\",\"\uD83E\uDDDA\",\"\uD83E\uDDDA\u200D♂\uFE0F\",\"\uD83E\uDDDA\u200D♀\uFE0F\",\"\uD83E\uDDC6\",\"\uD83C\uDDEB\uD83C\uDDF0\",\"\uD83C\uDF42\",\"\uD83D\uDC6A\",\"\uD83D\uDC68\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC66\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC67\",\"\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC67\",\"\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC67\",\"\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC67\",\"\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\",\"\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66\",\"\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67\",\"\uD83D\uDC69\u200D\uD83D\uDC66\",\"\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66\",\"\uD83D\uDC69\u200D\uD83D\uDC67\",\"\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66\",\"\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67\",\"\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC66\",\"\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66\",\"\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\",\"\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66\",\"\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67\",\"\uD83E\uDDD1\u200D\uD83C\uDF3E\",\"\uD83C\uDDEB\uD83C\uDDF4\",\"⏩\",\"\uD83D\uDCE0\",\"\uD83D\uDE28\",\"\uD83E\uDEB6\",\"\uD83D\uDC3E\",\"\uD83D\uDC3E\",\"\uD83D\uDD75\uFE0F\u200D♀\uFE0F\",\"♀\uFE0F\",\"\uD83C\uDFA1\",\"⛴\uFE0F\",\"\uD83C\uDFD1\",\"\uD83C\uDDEB\uD83C\uDDEF\",\"\uD83D\uDDC4\uFE0F\",\"\uD83D\uDCC1\",\"\uD83D\uDCFD\uFE0F\",\"\uD83C\uDF9E\uFE0F\",\"\uD83C\uDDEB\uD83C\uDDEE\",\"\uD83D\uDD25\",\"\uD83D\uDE92\",\"\uD83E\uDDEF\",\"\uD83E\uDDE8\",\"\uD83E\uDDD1\u200D\uD83D\uDE92\",\"\uD83C\uDF86\",\"\uD83C\uDF13\",\"\uD83C\uDF1B\",\"\uD83D\uDC1F\",\"\uD83C\uDF65\",\"\uD83C\uDFA3\",\"✊\",\"✊\",\"\uD83E\uDD1B\",\"\uD83E\uDD1C\",\"5\uFE0F⃣\",\"\uD83C\uDF8F\",\"\uD83E\uDDA9\",\"\uD83D\uDD26\",\"\uD83E\uDD7F\",\"\uD83E\uDED3\",\"⚜\uFE0F\",\"\uD83D\uDEEC\",\"\uD83D\uDEEB\",\"\uD83D\uDCBE\",\"\uD83C\uDFB4\",\"\uD83D\uDE33\",\"\uD83E\uDE88\",\"\uD83E\uDEB0\",\"\uD83E\uDD4F\",\"\uD83D\uDEF8\",\"\uD83C\uDF2B\uFE0F\",\"\uD83C\uDF01\",\"\uD83E\uDEAD\",\"\uD83E\uDED5\",\"\uD83E\uDDB6\",\"\uD83C\uDFC8\",\"\uD83D\uDC63\",\"\uD83C\uDF74\",\"\uD83E\uDD60\",\"⛲\",\"\uD83D\uDD8B\uFE0F\",\"4\uFE0F⃣\",\"\uD83C\uDF40\",\"\uD83E\uDD8A\",\"\uD83C\uDDEB\uD83C\uDDF7\",\"\uD83D\uDDBC\uFE0F\",\"\uD83C\uDD93\",\"\uD83C\uDDEC\uD83C\uDDEB\",\"\uD83C\uDDF5\uD83C\uDDEB\",\"\uD83C\uDDF9\uD83C\uDDEB\",\"\uD83C\uDF73\",\"\uD83C\uDF64\",\"\uD83C\uDF5F\",\"\uD83D\uDC38\",\"\uD83D\uDE26\",\"☹\uFE0F\",\"\uD83D\uDE4D\u200D♂\uFE0F\",\"\uD83D\uDE4D\",\"\uD83D\uDE4D\u200D♀\uFE0F\",\"\uD83D\uDD95\",\"\uD83D\uDD95\",\"⛽\",\"\uD83C\uDF15\",\"\uD83C\uDF1D\",\"⚱\uFE0F\",\"\uD83C\uDDEC\uD83C\uDDE6\",\"\uD83C\uDDEC\uD83C\uDDF2\",\"\uD83C\uDFB2\",\"\uD83E\uDDC4\",\"\uD83C\uDDEC\uD83C\uDDE7\",\"\uD83C\uDDEC\uD83C\uDDE7\",\"⚙\uFE0F\",\"\uD83D\uDC8E\",\"♊\",\"\uD83E\uDDDE\",\"\uD83E\uDDDE\u200D♂\uFE0F\",\"\uD83E\uDDDE\u200D♀\uFE0F\",\"\uD83C\uDDEC\uD83C\uDDEA\",\"\uD83C\uDDEC\uD83C\uDDED\",\"\uD83D\uDC7B\",\"\uD83C\uDDEC\uD83C\uDDEE\",\"\uD83C\uDF81\",\"\uD83D\uDC9D\",\"\uD83E\uDEDA\",\"\uD83E\uDD92\",\"\uD83D\uDC67\",\"\uD83C\uDF10\",\"\uD83E\uDDE4\",\"\uD83E\uDD45\",\"\uD83D\uDC10\",\"\uD83E\uDD7D\",\"⛳\",\"\uD83C\uDFCC\uFE0F\",\"\uD83C\uDFCC\uFE0F\u200D♂\uFE0F\",\"\uD83C\uDFCC\uFE0F\u200D♀\uFE0F\",\"\uD83E\uDEBF\",\"\uD83E\uDD8D\",\"\uD83C\uDF47\",\"\uD83C\uDDEC\uD83C\uDDF7\",\"\uD83C\uDF4F\",\"\uD83D\uDCD7\",\"\uD83D\uDFE2\",\"\uD83D\uDC9A\",\"\uD83E\uDD57\",\"\uD83D\uDFE9\",\"\uD83C\uDDEC\uD83C\uDDF1\",\"\uD83C\uDDEC\uD83C\uDDE9\",\"❕\",\"\uD83E\uDE76\",\"❔\",\"\uD83D\uDE2C\",\"\uD83D\uDE01\",\"\uD83D\uDE00\",\"\uD83C\uDDEC\uD83C\uDDF5\",\"\uD83C\uDDEC\uD83C\uDDFA\",\"\uD83D\uDC82\",\"\uD83D\uDC82\u200D♂\uFE0F\",\"\uD83D\uDC82\u200D♀\uFE0F\",\"\uD83C\uDDEC\uD83C\uDDF9\",\"\uD83C\uDDEC\uD83C\uDDEC\",\"\uD83E\uDDAE\",\"\uD83C\uDDEC\uD83C\uDDF3\",\"\uD83C\uDDEC\uD83C\uDDFC\",\"\uD83C\uDFB8\",\"\uD83D\uDD2B\",\"\uD83C\uDDEC\uD83C\uDDFE\",\"\uD83E\uDEAE\",\"\uD83D\uDC87\",\"\uD83D\uDC87\u200D♂\uFE0F\",\"\uD83D\uDC87\u200D♀\uFE0F\",\"\uD83C\uDDED\uD83C\uDDF9\",\"\uD83C\uDF54\",\"\uD83D\uDD28\",\"⚒\uFE0F\",\"\uD83D\uDEE0\uFE0F\",\"\uD83E\uDEAC\",\"\uD83D\uDC39\",\"✋\",\"✋\",\"\uD83E\uDD2D\",\"\uD83E\uDEF0\",\"\uD83D\uDC5C\",\"\uD83E\uDD3E\",\"\uD83E\uDD1D\",\"\uD83D\uDCA9\",\"\uD83D\uDCA9\",\"\uD83D\uDCA9\",\"#\uFE0F⃣\",\"\uD83D\uDC25\",\"\uD83D\uDC23\",\"\uD83C\uDFA7\",\"\uD83E\uDEA6\",\"\uD83E\uDDD1\u200D⚕\uFE0F\",\"\uD83D\uDE49\",\"\uD83C\uDDED\uD83C\uDDF2\",\"❤\uFE0F\",\"\uD83D\uDC9F\",\"\uD83D\uDE0D\",\"\uD83D\uDE3B\",\"\uD83E\uDEF6\",\"❤\uFE0F\u200D\uD83D\uDD25\",\"\uD83D\uDC93\",\"\uD83D\uDC97\",\"♥\uFE0F\",\"✔\uFE0F\",\"➗\",\"\uD83D\uDCB2\",\"\uD83D\uDFF0\",\"❣\uFE0F\",\"➖\",\"✖\uFE0F\",\"➕\",\"\uD83E\uDD94\",\"\uD83D\uDE81\",\"\uD83C\uDF3F\",\"\uD83C\uDF3A\",\"\uD83D\uDD06\",\"\uD83D\uDC60\",\"\uD83E\uDD7E\",\"\uD83D\uDED5\",\"\uD83E\uDD9B\",\"\uD83D\uDD2A\",\"\uD83D\uDD2A\",\"\uD83D\uDD73\uFE0F\",\"\uD83C\uDDED\uD83C\uDDF3\",\"\uD83C\uDF6F\",\"\uD83C\uDDED\uD83C\uDDF0\",\"\uD83E\uDE9D\",\"\uD83D\uDC34\",\"\uD83C\uDFC7\",\"\uD83C\uDFE5\",\"\uD83E\uDD75\",\"\uD83C\uDF36\uFE0F\",\"\uD83C\uDF2D\",\"\uD83C\uDFE8\",\"♨\uFE0F\",\"⌛\",\"⏳\",\"\uD83C\uDFE0\",\"\uD83C\uDFE1\",\"\uD83C\uDFD8\uFE0F\",\"\uD83E\uDD17\",\"\uD83C\uDDED\uD83C\uDDFA\",\"\uD83D\uDE2F\",\"\uD83D\uDED6\",\"\uD83E\uDEBB\",\"\uD83C\uDF68\",\"\uD83E\uDDCA\",\"\uD83C\uDFD2\",\"⛸\uFE0F\",\"\uD83C\uDF66\",\"\uD83C\uDDEE\uD83C\uDDF8\",\"\uD83C\uDD94\",\"\uD83E\uDEAA\",\"\uD83C\uDE50\",\"\uD83D\uDC7F\",\"\uD83D\uDCE5\",\"\uD83D\uDCE8\",\"\uD83E\uDEF5\",\"\uD83C\uDDEE\uD83C\uDDF3\",\"\uD83C\uDDEE\uD83C\uDDE9\",\"♾\uFE0F\",\"\uD83D\uDC81\",\"\uD83D\uDC81\",\"ℹ\uFE0F\",\"\uD83D\uDE07\",\"⁉\uFE0F\",\"\uD83D\uDCF1\",\"\uD83C\uDDEE\uD83C\uDDF7\",\"\uD83C\uDDEE\uD83C\uDDF6\",\"\uD83C\uDDEE\uD83C\uDDEA\",\"\uD83C\uDDEE\uD83C\uDDF2\",\"\uD83C\uDDEE\uD83C\uDDF1\",\"\uD83C\uDDEE\uD83C\uDDF9\",\"\uD83C\uDFEE\",\"\uD83C\uDFEE\",\"\uD83C\uDF83\",\"\uD83C\uDDEF\uD83C\uDDF2\",\"\uD83D\uDDFE\",\"\uD83C\uDFEF\",\"\uD83D\uDC7A\",\"\uD83D\uDC79\",\"\uD83E\uDED9\",\"\uD83D\uDC56\",\"\uD83E\uDEBC\",\"\uD83C\uDDEF\uD83C\uDDEA\",\"\uD83E\uDDE9\",\"\uD83C\uDDEF\uD83C\uDDF4\",\"\uD83D\uDE02\",\"\uD83D\uDE39\",\"\uD83D\uDD79\uFE0F\",\"\uD83C\uDDEF\uD83C\uDDF5\",\"\uD83E\uDDD1\u200D⚖\uFE0F\",\"\uD83E\uDD39\",\"\uD83D\uDD4B\",\"\uD83E\uDD98\",\"\uD83C\uDDF0\uD83C\uDDFF\",\"\uD83C\uDDF0\uD83C\uDDEA\",\"\uD83D\uDD11\",\"⌨\uFE0F\",\"\uD83D\uDD1F\",\"\uD83E\uDEAF\",\"\uD83D\uDEF4\",\"\uD83D\uDC58\",\"\uD83C\uDDF0\uD83C\uDDEE\",\"\uD83D\uDC8B\",\"\uD83D\uDE17\",\"\uD83D\uDE3D\",\"\uD83D\uDE1A\",\"\uD83D\uDE18\",\"\uD83D\uDE19\",\"\uD83E\uDE81\",\"\uD83E\uDD5D\",\"\uD83E\uDDCE\u200D♂\uFE0F\",\"\uD83E\uDDCE\",\"\uD83E\uDDCE\u200D♀\uFE0F\",\"\uD83E\uDEA2\",\"\uD83D\uDC28\",\"\uD83C\uDE01\",\"\uD83C\uDDFD\uD83C\uDDF0\",\"\uD83C\uDDF0\uD83C\uDDF7\",\"\uD83C\uDDF0\uD83C\uDDFC\",\"\uD83C\uDDF0\uD83C\uDDEC\",\"\uD83E\uDD7C\",\"\uD83C\uDFF7\uFE0F\",\"\uD83E\uDD4D\",\"\uD83E\uDE9C\",\"\uD83D\uDC1E\",\"\uD83C\uDDF1\uD83C\uDDE6\",\"\uD83D\uDD35\",\"\uD83D\uDD37\",\"\uD83D\uDD36\",\"\uD83C\uDF17\",\"\uD83C\uDF1C\",\"✝\uFE0F\",\"\uD83C\uDDF1\uD83C\uDDFB\",\"\uD83D\uDE06\",\"\uD83D\uDE06\",\"\uD83E\uDD6C\",\"\uD83C\uDF43\",\"\uD83C\uDDF1\uD83C\uDDE7\",\"\uD83D\uDCD2\",\"\uD83D\uDEC5\",\"↔\uFE0F\",\"\uD83D\uDDE8\uFE0F\",\"↩\uFE0F\",\"\uD83E\uDEF2\",\"\uD83E\uDEF7\",\"\uD83E\uDDB5\",\"\uD83C\uDF4B\",\"♌\",\"\uD83D\uDC06\",\"\uD83C\uDDF1\uD83C\uDDF8\",\"\uD83C\uDF9A\uFE0F\",\"\uD83C\uDDF1\uD83C\uDDF7\",\"♎\",\"\uD83C\uDDF1\uD83C\uDDFE\",\"\uD83C\uDDF1\uD83C\uDDEE\",\"\uD83E\uDE75\",\"\uD83D\uDE88\",\"\uD83D\uDD17\",\"\uD83E\uDD81\",\"\uD83D\uDC44\",\"\uD83D\uDC84\",\"\uD83C\uDDF1\uD83C\uDDF9\",\"\uD83E\uDD8E\",\"\uD83E\uDD99\",\"\uD83E\uDD9E\",\"\uD83D\uDD12\",\"\uD83D\uDD0F\",\"\uD83C\uDF6D\",\"\uD83E\uDE98\",\"➿\",\"\uD83E\uDDF4\",\"\uD83E\uDEB7\",\"\uD83E\uDDD8\",\"\uD83E\uDDD8\u200D♂\uFE0F\",\"\uD83E\uDDD8\u200D♀\uFE0F\",\"\uD83D\uDD0A\",\"\uD83D\uDCE2\",\"\uD83C\uDFE9\",\"\uD83D\uDC8C\",\"\uD83E\uDD1F\",\"\uD83E\uDEAB\",\"\uD83D\uDD05\",\"\uD83E\uDDF3\",\"\uD83E\uDEC1\",\"\uD83C\uDDF1\uD83C\uDDFA\",\"\uD83E\uDD25\",\"Ⓜ\uFE0F\",\"\uD83C\uDDF2\uD83C\uDDF4\",\"\uD83C\uDDF2\uD83C\uDDF0\",\"\uD83C\uDDF2\uD83C\uDDEC\",\"\uD83D\uDD0D\",\"\uD83D\uDD0E\",\"\uD83E\uDDD9\",\"\uD83E\uDDD9\u200D♂\uFE0F\",\"\uD83E\uDDD9\u200D♀\uFE0F\",\"\uD83E\uDE84\",\"\uD83E\uDDF2\",\"\uD83C\uDC04\",\"\uD83D\uDCEB\",\"\uD83D\uDCEA\",\"\uD83D\uDCEC\",\"\uD83D\uDCED\",\"\uD83C\uDDF2\uD83C\uDDFC\",\"\uD83C\uDDF2\uD83C\uDDFE\",\"\uD83C\uDDF2\uD83C\uDDFB\",\"\uD83D\uDD75\uFE0F\u200D♂\uFE0F\",\"♂\uFE0F\",\"\uD83C\uDDF2\uD83C\uDDF1\",\"\uD83C\uDDF2\uD83C\uDDF9\",\"\uD83E\uDDA3\",\"\uD83D\uDC68\",\"\uD83D\uDC68\u200D\uD83C\uDFA8\",\"\uD83D\uDC68\u200D\uD83D\uDE80\",\"\uD83E\uDDD4\u200D♂\uFE0F\",\"\uD83E\uDD38\u200D♂\uFE0F\",\"\uD83D\uDC68\u200D\uD83C\uDF73\",\"\uD83D\uDD7A\",\"\uD83E\uDD26\u200D♂\uFE0F\",\"\uD83D\uDC68\u200D\uD83C\uDFED\",\"\uD83D\uDC68\u200D\uD83C\uDF3E\",\"\uD83D\uDC68\u200D\uD83C\uDF7C\",\"\uD83D\uDC68\u200D\uD83D\uDE92\",\"\uD83D\uDC68\u200D⚕\uFE0F\",\"\uD83D\uDC68\u200D\uD83E\uDDBD\",\"\uD83D\uDC68\u200D\uD83E\uDDBC\",\"\uD83E\uDD35\u200D♂\uFE0F\",\"\uD83D\uDC68\u200D⚖\uFE0F\",\"\uD83E\uDD39\u200D♂\uFE0F\",\"\uD83D\uDC68\u200D\uD83D\uDD27\",\"\uD83D\uDC68\u200D\uD83D\uDCBC\",\"\uD83D\uDC68\u200D✈\uFE0F\",\"\uD83E\uDD3E\u200D♂\uFE0F\",\"\uD83E\uDD3D\u200D♂\uFE0F\",\"\uD83D\uDC68\u200D\uD83D\uDD2C\",\"\uD83E\uDD37\u200D♂\uFE0F\",\"\uD83D\uDC68\u200D\uD83C\uDFA4\",\"\uD83D\uDC68\u200D\uD83C\uDF93\",\"\uD83D\uDC68\u200D\uD83C\uDFEB\",\"\uD83D\uDC68\u200D\uD83D\uDCBB\",\"\uD83D\uDC72\",\"\uD83D\uDC68\u200D\uD83E\uDDAF\",\"\uD83D\uDC73\u200D♂\uFE0F\",\"\uD83D\uDC70\u200D♂\uFE0F\",\"\uD83C\uDF4A\",\"\uD83C\uDF4A\",\"\uD83C\uDF4A\",\"\uD83E\uDD6D\",\"\uD83D\uDC5E\",\"\uD83D\uDC5E\",\"\uD83D\uDD70\uFE0F\",\"\uD83E\uDDBD\",\"\uD83C\uDF41\",\"\uD83E\uDE87\",\"\uD83C\uDDF2\uD83C\uDDED\",\"\uD83E\uDD4B\",\"\uD83C\uDDF2\uD83C\uDDF6\",\"\uD83D\uDE37\",\"\uD83D\uDC86\",\"\uD83D\uDC86\u200D♂\uFE0F\",\"\uD83D\uDC86\u200D♀\uFE0F\",\"\uD83E\uDDC9\",\"\uD83C\uDDF2\uD83C\uDDF7\",\"\uD83C\uDDF2\uD83C\uDDFA\",\"\uD83C\uDDFE\uD83C\uDDF9\",\"\uD83C\uDF56\",\"\uD83E\uDDD1\u200D\uD83D\uDD27\",\"\uD83E\uDDBE\",\"\uD83E\uDDBF\",\"\uD83C\uDF96\uFE0F\",\"\uD83C\uDFC5\",\"⚕\uFE0F\",\"\uD83D\uDCE3\",\"\uD83C\uDF48\",\"\uD83E\uDEE0\",\"\uD83D\uDCDD\",\"\uD83D\uDCDD\",\"\uD83E\uDD3C\u200D♂\uFE0F\",\"❤\uFE0F\u200D\uD83E\uDE79\",\"\uD83D\uDD4E\",\"\uD83D\uDEB9\",\"\uD83E\uDDDC\u200D♀\uFE0F\",\"\uD83E\uDDDC\u200D♂\uFE0F\",\"\uD83E\uDDDC\",\"\uD83E\uDD18\",\"\uD83D\uDE87\",\"\uD83C\uDDF2\uD83C\uDDFD\",\"\uD83E\uDDA0\",\"\uD83C\uDDEB\uD83C\uDDF2\",\"\uD83C\uDFA4\",\"\uD83D\uDD2C\",\"\uD83E\uDE96\",\"\uD83E\uDD5B\",\"\uD83C\uDF0C\",\"\uD83D\uDE90\",\"\uD83D\uDCBD\",\"\uD83E\uDE9E\",\"\uD83E\uDEA9\",\"\uD83D\uDCF4\",\"\uD83C\uDDF2\uD83C\uDDE9\",\"\uD83C\uDDF2\uD83C\uDDE8\",\"\uD83E\uDD11\",\"\uD83D\uDCB8\",\"\uD83D\uDCB0\",\"\uD83C\uDDF2\uD83C\uDDF3\",\"\uD83D\uDC12\",\"\uD83D\uDC35\",\"\uD83E\uDDD0\",\"\uD83D\uDE9D\",\"\uD83C\uDDF2\uD83C\uDDEA\",\"\uD83C\uDDF2\uD83C\uDDF8\",\"\uD83C\uDF14\",\"\uD83C\uDF14\",\"\uD83E\uDD6E\",\"\uD83E\uDECE\",\"\uD83C\uDDF2\uD83C\uDDE6\",\"\uD83C\uDF93\",\"\uD83D\uDD4C\",\"\uD83E\uDD9F\",\"\uD83D\uDEE5\uFE0F\",\"\uD83D\uDEF5\",\"\uD83C\uDFCD\uFE0F\",\"\uD83E\uDDBC\",\"\uD83D\uDEE3\uFE0F\",\"\uD83D\uDDFB\",\"⛰\uFE0F\",\"\uD83D\uDEB5\",\"\uD83D\uDEB5\u200D♂\uFE0F\",\"\uD83D\uDEB5\u200D♀\uFE0F\",\"\uD83D\uDEA0\",\"\uD83D\uDE9E\",\"\uD83C\uDFD4\uFE0F\",\"\uD83D\uDC2D\",\"\uD83D\uDC01\",\"\uD83E\uDEA4\",\"\uD83C\uDFA5\",\"\uD83D\uDDFF\",\"\uD83C\uDDF2\uD83C\uDDFF\",\"\uD83E\uDD36\",\"\uD83D\uDCAA\",\"\uD83C\uDF44\",\"\uD83C\uDFB9\",\"\uD83C\uDFB5\",\"\uD83C\uDFBC\",\"\uD83D\uDD07\",\"\uD83E\uDDD1\u200D\uD83C\uDF84\",\"\uD83C\uDDF2\uD83C\uDDF2\",\"\uD83D\uDC85\",\"\uD83D\uDCDB\",\"\uD83C\uDDF3\uD83C\uDDE6\",\"\uD83C\uDFDE\uFE0F\",\"\uD83C\uDDF3\uD83C\uDDF7\",\"\uD83E\uDD22\",\"\uD83E\uDDFF\",\"\uD83D\uDC54\",\"❎\",\"\uD83C\uDDF3\uD83C\uDDF5\",\"\uD83E\uDD13\",\"\uD83E\uDEBA\",\"\uD83E\uDE86\",\"\uD83C\uDDF3\uD83C\uDDF1\",\"\uD83D\uDE10\",\"\uD83C\uDD95\",\"\uD83C\uDDF3\uD83C\uDDE8\",\"\uD83C\uDF11\",\"\uD83C\uDF1A\",\"\uD83C\uDDF3\uD83C\uDDFF\",\"\uD83D\uDCF0\",\"\uD83D\uDDDE\uFE0F\",\"⏭\uFE0F\",\"\uD83C\uDD96\",\"\uD83D\uDE45\u200D♂\uFE0F\",\"\uD83D\uDE45\u200D♂\uFE0F\",\"\uD83D\uDE45\u200D♀\uFE0F\",\"\uD83D\uDE45\u200D♀\uFE0F\",\"\uD83C\uDDF3\uD83C\uDDEE\",\"\uD83C\uDDF3\uD83C\uDDEA\",\"\uD83C\uDDF3\uD83C\uDDEC\",\"\uD83C\uDF03\",\"9\uFE0F⃣\",\"\uD83E\uDD77\",\"\uD83C\uDDF3\uD83C\uDDFA\",\"\uD83D\uDD15\",\"\uD83D\uDEB3\",\"⛔\",\"\uD83D\uDEAB\",\"\uD83D\uDE45\",\"\uD83D\uDCF5\",\"\uD83D\uDE36\",\"\uD83D\uDEB7\",\"\uD83D\uDEAD\",\"\uD83D\uDEB1\",\"\uD83C\uDDF3\uD83C\uDDEB\",\"\uD83C\uDDF0\uD83C\uDDF5\",\"\uD83C\uDDF2\uD83C\uDDF5\",\"\uD83C\uDDF3\uD83C\uDDF4\",\"\uD83D\uDC43\",\"\uD83D\uDCD3\",\"\uD83D\uDCD4\",\"\uD83C\uDFB6\",\"\uD83D\uDD29\",\"⭕\",\"\uD83C\uDD7E\uFE0F\",\"\uD83C\uDF0A\",\"\uD83D\uDC19\",\"\uD83C\uDF62\",\"\uD83C\uDFE2\",\"\uD83E\uDDD1\u200D\uD83D\uDCBC\",\"\uD83D\uDEE2\uFE0F\",\"\uD83C\uDD97\",\"\uD83D\uDC4C\",\"\uD83D\uDE46\u200D♂\uFE0F\",\"\uD83D\uDE46\",\"\uD83D\uDE46\u200D♀\uFE0F\",\"\uD83D\uDDDD\uFE0F\",\"\uD83E\uDDD3\",\"\uD83D\uDC74\",\"\uD83D\uDC75\",\"\uD83E\uDED2\",\"\uD83D\uDD49\uFE0F\",\"\uD83C\uDDF4\uD83C\uDDF2\",\"\uD83D\uDD1B\",\"\uD83D\uDE98\",\"\uD83D\uDE8D\",\"\uD83D\uDE94\",\"\uD83D\uDE96\",\"1\uFE0F⃣\",\"\uD83E\uDE71\",\"\uD83E\uDDC5\",\"\uD83D\uDCC2\",\"\uD83D\uDC50\",\"\uD83D\uDE2E\",\"☂\uFE0F\",\"⛎\",\"\uD83D\uDCD9\",\"\uD83D\uDFE0\",\"\uD83E\uDDE1\",\"\uD83D\uDFE7\",\"\uD83E\uDDA7\",\"☦\uFE0F\",\"\uD83E\uDDA6\",\"\uD83D\uDCE4\",\"\uD83E\uDD89\",\"\uD83D\uDC02\",\"\uD83E\uDDAA\",\"\uD83D\uDCE6\",\"\uD83D\uDCC4\",\"\uD83D\uDCC3\",\"\uD83D\uDCDF\",\"\uD83D\uDD8C\uFE0F\",\"\uD83C\uDDF5\uD83C\uDDF0\",\"\uD83C\uDDF5\uD83C\uDDFC\",\"\uD83C\uDDF5\uD83C\uDDF8\",\"\uD83E\uDEF3\",\"\uD83C\uDF34\",\"\uD83E\uDEF4\",\"\uD83E\uDD32\",\"\uD83C\uDDF5\uD83C\uDDE6\",\"\uD83E\uDD5E\",\"\uD83D\uDC3C\",\"\uD83D\uDCCE\",\"\uD83D\uDD87\uFE0F\",\"\uD83C\uDDF5\uD83C\uDDEC\",\"\uD83E\uDE82\",\"\uD83C\uDDF5\uD83C\uDDFE\",\"⛱\uFE0F\",\"\uD83C\uDD7F\uFE0F\",\"\uD83E\uDD9C\",\"〽\uFE0F\",\"⛅\",\"\uD83E\uDD73\",\"\uD83D\uDEF3\uFE0F\",\"\uD83D\uDEC2\",\"⏸\uFE0F\",\"\uD83E\uDEDB\",\"☮\uFE0F\",\"\uD83C\uDF51\",\"\uD83E\uDD9A\",\"\uD83E\uDD5C\",\"\uD83C\uDF50\",\"\uD83D\uDD8A\uFE0F\",\"✏\uFE0F\",\"\uD83D\uDC27\",\"\uD83D\uDE14\",\"\uD83E\uDDD1\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1\",\"\uD83E\uDEC2\",\"\uD83C\uDFAD\",\"\uD83D\uDE23\",\"\uD83E\uDDD1\u200D\uD83E\uDDB2\",\"\uD83E\uDDD1\u200D\uD83E\uDDB1\",\"\uD83E\uDDD1\u200D\uD83C\uDF7C\",\"\uD83E\uDD3A\",\"\uD83E\uDDD1\u200D\uD83E\uDDBD\",\"\uD83E\uDDD1\u200D\uD83E\uDDBC\",\"\uD83E\uDD35\",\"\uD83E\uDDD1\u200D\uD83E\uDDB0\",\"\uD83E\uDDD1\u200D\uD83E\uDDB3\",\"\uD83E\uDEC5\",\"\uD83E\uDDD1\u200D\uD83E\uDDAF\",\"\uD83D\uDC73\",\"\uD83D\uDC70\",\"\uD83C\uDDF5\uD83C\uDDEA\",\"\uD83E\uDDEB\",\"\uD83C\uDDF5\uD83C\uDDED\",\"☎\uFE0F\",\"☎\uFE0F\",\"⛏\uFE0F\",\"\uD83D\uDEFB\",\"\uD83E\uDD67\",\"\uD83D\uDC37\",\"\uD83D\uDC16\",\"\uD83D\uDC3D\",\"\uD83D\uDC8A\",\"\uD83E\uDDD1\u200D✈\uFE0F\",\"\uD83E\uDE85\",\"\uD83E\uDD0C\",\"\uD83E\uDD0F\",\"\uD83C\uDF4D\",\"\uD83C\uDFD3\",\"\uD83E\uDE77\",\"\uD83C\uDFF4\u200D☠\uFE0F\",\"♓\",\"\uD83C\uDDF5\uD83C\uDDF3\",\"\uD83C\uDF55\",\"\uD83E\uDEA7\",\"\uD83D\uDED0\",\"\uD83C\uDF7D\uFE0F\",\"⏯\uFE0F\",\"\uD83D\uDEDD\",\"\uD83E\uDD7A\",\"\uD83E\uDEA0\",\"\uD83D\uDC47\",\"\uD83D\uDC48\",\"\uD83D\uDC49\",\"☝\uFE0F\",\"\uD83D\uDC46\",\"\uD83C\uDDF5\uD83C\uDDF1\",\"\uD83D\uDC3B\u200D❄\uFE0F\",\"\uD83D\uDE93\",\"\uD83D\uDC6E\u200D♂\uFE0F\",\"\uD83D\uDC6E\u200D♀\uFE0F\",\"\uD83D\uDC29\",\"\uD83C\uDF7F\",\"\uD83C\uDDF5\uD83C\uDDF9\",\"\uD83C\uDFE3\",\"\uD83D\uDCEF\",\"\uD83D\uDCEE\",\"\uD83D\uDEB0\",\"\uD83E\uDD54\",\"\uD83E\uDEB4\",\"\uD83D\uDC5D\",\"\uD83C\uDF57\",\"\uD83D\uDCB7\",\"\uD83E\uDED7\",\"\uD83D\uDE21\",\"\uD83D\uDE21\",\"\uD83D\uDE3E\",\"\uD83D\uDE4E\",\"\uD83D\uDE4E\u200D♂\uFE0F\",\"\uD83D\uDE4E\u200D♀\uFE0F\",\"\uD83D\uDE4F\",\"\uD83D\uDCFF\",\"\uD83E\uDEC3\",\"\uD83E\uDEC4\",\"\uD83E\uDD30\",\"\uD83E\uDD68\",\"⏮\uFE0F\",\"\uD83E\uDD34\",\"\uD83D\uDC78\",\"\uD83D\uDDA8\uFE0F\",\"\uD83E\uDDAF\",\"\uD83C\uDDF5\uD83C\uDDF7\",\"\uD83D\uDFE3\",\"\uD83D\uDC9C\",\"\uD83D\uDFEA\",\"\uD83D\uDC5B\",\"\uD83D\uDCCC\",\"\uD83D\uDEAE\",\"\uD83C\uDDF6\uD83C\uDDE6\",\"❓\",\"\uD83D\uDC30\",\"\uD83D\uDC07\",\"\uD83E\uDD9D\",\"\uD83D\uDC0E\",\"\uD83C\uDFCE\uFE0F\",\"\uD83D\uDCFB\",\"\uD83D\uDD18\",\"☢\uFE0F\",\"\uD83D\uDE83\",\"\uD83D\uDEE4\uFE0F\",\"\uD83C\uDF08\",\"\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08\",\"\uD83E\uDD1A\",\"\uD83E\uDD28\",\"\uD83D\uDD90\uFE0F\",\"\uD83D\uDE4C\",\"\uD83D\uDE4B\",\"\uD83D\uDE4B\u200D♂\uFE0F\",\"\uD83D\uDE4B\u200D♀\uFE0F\",\"\uD83D\uDC0F\",\"\uD83C\uDF5C\",\"\uD83D\uDC00\",\"\uD83E\uDE92\",\"\uD83E\uDDFE\",\"⏺\uFE0F\",\"♻\uFE0F\",\"\uD83D\uDD34\",\"\uD83E\uDDE7\",\"\uD83D\uDC68\u200D\uD83E\uDDB0\",\"\uD83D\uDC69\u200D\uD83E\uDDB0\",\"\uD83D\uDFE5\",\"®\uFE0F\",\"☺\uFE0F\",\"\uD83D\uDE0C\",\"\uD83C\uDF97\uFE0F\",\"\uD83D\uDD01\",\"\uD83D\uDD02\",\"⛑\uFE0F\",\"\uD83D\uDEBB\",\"\uD83C\uDDF7\uD83C\uDDEA\",\"\uD83D\uDC9E\",\"⏪\",\"\uD83E\uDD8F\",\"\uD83C\uDF80\",\"\uD83C\uDF5A\",\"\uD83C\uDF59\",\"\uD83C\uDF58\",\"\uD83C\uDF91\",\"\uD83D\uDDEF\uFE0F\",\"\uD83E\uDEF1\",\"\uD83E\uDEF8\",\"\uD83D\uDC8D\",\"\uD83D\uDEDF\",\"\uD83E\uDE90\",\"\uD83E\uDD16\",\"\uD83E\uDEA8\",\"\uD83D\uDE80\",\"\uD83E\uDD23\",\"\uD83D\uDE44\",\"\uD83E\uDDFB\",\"\uD83C\uDFA2\",\"\uD83D\uDEFC\",\"\uD83C\uDDF7\uD83C\uDDF4\",\"\uD83D\uDC13\",\"\uD83C\uDF39\",\"\uD83C\uDFF5\uFE0F\",\"\uD83D\uDEA8\",\"\uD83D\uDCCD\",\"\uD83D\uDEA3\",\"\uD83D\uDEA3\u200D♂\uFE0F\",\"\uD83D\uDEA3\u200D♀\uFE0F\",\"\uD83C\uDDF7\uD83C\uDDFA\",\"\uD83C\uDFC9\",\"\uD83C\uDFC3\",\"\uD83C\uDFC3\",\"\uD83C\uDFC3\u200D♂\uFE0F\",\"\uD83C\uDFBD\",\"\uD83C\uDFC3\u200D♀\uFE0F\",\"\uD83C\uDDF7\uD83C\uDDFC\",\"\uD83C\uDE02\uFE0F\",\"\uD83E\uDDF7\",\"\uD83E\uDDBA\",\"♐\",\"\uD83C\uDF76\",\"\uD83E\uDDC2\",\"\uD83E\uDEE1\",\"\uD83C\uDDFC\uD83C\uDDF8\",\"\uD83C\uDDF8\uD83C\uDDF2\",\"\uD83D\uDC61\",\"\uD83E\uDD6A\",\"\uD83C\uDF85\",\"\uD83C\uDDF8\uD83C\uDDF9\",\"\uD83E\uDD7B\",\"\uD83D\uDC81\u200D♂\uFE0F\",\"\uD83D\uDC81\u200D♂\uFE0F\",\"\uD83D\uDC81\u200D♀\uFE0F\",\"\uD83D\uDC81\u200D♀\uFE0F\",\"\uD83D\uDCE1\",\"\uD83C\uDDF8\uD83C\uDDE6\",\"\uD83E\uDDD6\u200D♂\uFE0F\",\"\uD83E\uDDD6\",\"\uD83E\uDDD6\u200D♀\uFE0F\",\"\uD83E\uDD95\",\"\uD83C\uDFB7\",\"\uD83E\uDDE3\",\"\uD83C\uDFEB\",\"\uD83C\uDF92\",\"\uD83E\uDDD1\u200D\uD83D\uDD2C\",\"✂\uFE0F\",\"\uD83E\uDD82\",\"♏\",\"\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC73\uDB40\uDC63\uDB40\uDC74\uDB40\uDC7F\",\"\uD83D\uDE31\",\"\uD83D\uDE40\",\"\uD83E\uDE9B\",\"\uD83D\uDCDC\",\"\uD83E\uDDAD\",\"\uD83D\uDCBA\",\"㊙\uFE0F\",\"\uD83D\uDE48\",\"\uD83C\uDF31\",\"\uD83E\uDD33\",\"\uD83C\uDDF8\uD83C\uDDF3\",\"\uD83C\uDDF7\uD83C\uDDF8\",\"\uD83D\uDC15\u200D\uD83E\uDDBA\",\"7\uFE0F⃣\",\"\uD83E\uDEA1\",\"\uD83C\uDDF8\uD83C\uDDE8\",\"\uD83E\uDEE8\",\"\uD83E\uDD58\",\"☘\uFE0F\",\"\uD83E\uDD88\",\"\uD83C\uDF67\",\"\uD83D\uDC11\",\"\uD83D\uDC1A\",\"\uD83D\uDEE1\uFE0F\",\"⛩\uFE0F\",\"\uD83D\uDEA2\",\"\uD83D\uDC55\",\"\uD83D\uDC55\",\"\uD83D\uDECD\uFE0F\",\"\uD83D\uDED2\",\"\uD83E\uDE73\",\"\uD83D\uDEBF\",\"\uD83E\uDD90\",\"\uD83E\uDD37\",\"\uD83E\uDD2B\",\"\uD83C\uDDF8\uD83C\uDDF1\",\"\uD83D\uDCF6\",\"\uD83C\uDDF8\uD83C\uDDEC\",\"\uD83E\uDDD1\u200D\uD83C\uDFA4\",\"\uD83C\uDDF8\uD83C\uDDFD\",\"6\uFE0F⃣\",\"\uD83D\uDD2F\",\"\uD83D\uDEF9\",\"\uD83C\uDFBF\",\"⛷\uFE0F\",\"\uD83D\uDC80\",\"☠\uFE0F\",\"\uD83E\uDDA8\",\"\uD83D\uDEF7\",\"\uD83D\uDE34\",\"\uD83D\uDECC\",\"\uD83D\uDE2A\",\"\uD83D\uDE41\",\"\uD83D\uDE42\",\"\uD83C\uDFB0\",\"\uD83E\uDDA5\",\"\uD83C\uDDF8\uD83C\uDDF0\",\"\uD83C\uDDF8\uD83C\uDDEE\",\"\uD83D\uDEE9\uFE0F\",\"\uD83D\uDD39\",\"\uD83D\uDD38\",\"\uD83D\uDD3A\",\"\uD83D\uDD3B\",\"\uD83D\uDE04\",\"\uD83D\uDE38\",\"\uD83D\uDE03\",\"\uD83D\uDE3A\",\"\uD83E\uDD72\",\"\uD83E\uDD70\",\"\uD83D\uDE08\",\"\uD83D\uDE0F\",\"\uD83D\uDE3C\",\"\uD83D\uDEAC\",\"\uD83D\uDC0C\",\"\uD83D\uDC0D\",\"\uD83E\uDD27\",\"\uD83C\uDFC2\",\"❄\uFE0F\",\"⛄\",\"☃\uFE0F\",\"\uD83E\uDDFC\",\"\uD83D\uDE2D\",\"⚽\",\"\uD83E\uDDE6\",\"\uD83E\uDD4E\",\"\uD83C\uDDF8\uD83C\uDDE7\",\"\uD83C\uDDF8\uD83C\uDDF4\",\"\uD83D\uDD1C\",\"\uD83C\uDD98\",\"\uD83D\uDD09\",\"\uD83C\uDDFF\uD83C\uDDE6\",\"\uD83C\uDDEC\uD83C\uDDF8\",\"\uD83C\uDDF8\uD83C\uDDF8\",\"\uD83D\uDC7E\",\"♠\uFE0F\",\"\uD83C\uDF5D\",\"❇\uFE0F\",\"\uD83C\uDF87\",\"✨\",\"\uD83D\uDC96\",\"\uD83D\uDE4A\",\"\uD83D\uDD08\",\"\uD83D\uDDE3\uFE0F\",\"\uD83D\uDCAC\",\"\uD83D\uDEA4\",\"\uD83D\uDD77\uFE0F\",\"\uD83D\uDD78\uFE0F\",\"\uD83D\uDDD3\uFE0F\",\"\uD83D\uDDD2\uFE0F\",\"\uD83E\uDDFD\",\"\uD83E\uDD44\",\"\uD83E\uDD91\",\"\uD83C\uDDF1\uD83C\uDDF0\",\"\uD83C\uDDE7\uD83C\uDDF1\",\"\uD83C\uDDF8\uD83C\uDDED\",\"\uD83C\uDDF0\uD83C\uDDF3\",\"\uD83C\uDDF1\uD83C\uDDE8\",\"\uD83C\uDDF2\uD83C\uDDEB\",\"\uD83C\uDDF5\uD83C\uDDF2\",\"\uD83C\uDDFB\uD83C\uDDE8\",\"\uD83C\uDFDF\uFE0F\",\"\uD83E\uDDCD\u200D♂\uFE0F\",\"\uD83E\uDDCD\",\"\uD83E\uDDCD\u200D♀\uFE0F\",\"⭐\",\"\uD83C\uDF1F\",\"☪\uFE0F\",\"✡\uFE0F\",\"\uD83E\uDD29\",\"\uD83C\uDF20\",\"\uD83D\uDE89\",\"\uD83D\uDDFD\",\"\uD83D\uDE82\",\"\uD83E\uDE7A\",\"\uD83C\uDF72\",\"⏹\uFE0F\",\"\uD83D\uDED1\",\"⏱\uFE0F\",\"\uD83D\uDCCF\",\"\uD83C\uDF53\",\"\uD83D\uDE1B\",\"\uD83D\uDE1D\",\"\uD83D\uDE1C\",\"\uD83E\uDDD1\u200D\uD83C\uDF93\",\"\uD83C\uDF99\uFE0F\",\"\uD83E\uDD59\",\"\uD83C\uDDF8\uD83C\uDDE9\",\"\uD83C\uDF25\uFE0F\",\"\uD83C\uDF26\uFE0F\",\"\uD83C\uDF24\uFE0F\",\"\uD83C\uDF1E\",\"\uD83C\uDF3B\",\"\uD83D\uDE0E\",\"☀\uFE0F\",\"\uD83C\uDF05\",\"\uD83C\uDF04\",\"\uD83E\uDDB8\",\"\uD83E\uDDB8\u200D♂\uFE0F\",\"\uD83E\uDDB8\u200D♀\uFE0F\",\"\uD83E\uDDB9\",\"\uD83E\uDDB9\u200D♂\uFE0F\",\"\uD83E\uDDB9\u200D♀\uFE0F\",\"\uD83C\uDFC4\",\"\uD83C\uDFC4\u200D♂\uFE0F\",\"\uD83C\uDFC4\u200D♀\uFE0F\",\"\uD83C\uDDF8\uD83C\uDDF7\",\"\uD83C\uDF63\",\"\uD83D\uDE9F\",\"\uD83C\uDDF8\uD83C\uDDEF\",\"\uD83E\uDDA2\",\"\uD83C\uDDF8\uD83C\uDDFF\",\"\uD83D\uDE13\",\"\uD83D\uDCA6\",\"\uD83D\uDE05\",\"\uD83C\uDDF8\uD83C\uDDEA\",\"\uD83C\uDF60\",\"\uD83E\uDE72\",\"\uD83C\uDFCA\",\"\uD83C\uDFCA\u200D♂\uFE0F\",\"\uD83C\uDFCA\u200D♀\uFE0F\",\"\uD83C\uDDE8\uD83C\uDDED\",\"\uD83D\uDD23\",\"\uD83D\uDD4D\",\"\uD83C\uDDF8\uD83C\uDDFE\",\"\uD83D\uDC89\",\"\uD83E\uDD96\",\"\uD83C\uDF2E\",\"\uD83C\uDF89\",\"\uD83C\uDDF9\uD83C\uDDFC\",\"\uD83C\uDDF9\uD83C\uDDEF\",\"\uD83E\uDD61\",\"\uD83E\uDED4\",\"\uD83C\uDF8B\",\"\uD83C\uDDF9\uD83C\uDDFF\",\"♉\",\"\uD83D\uDE95\",\"\uD83C\uDF75\",\"\uD83E\uDDD1\u200D\uD83C\uDFEB\",\"\uD83E\uDED6\",\"\uD83E\uDDD1\u200D\uD83D\uDCBB\",\"\uD83E\uDDF8\",\"\uD83D\uDCDE\",\"\uD83D\uDD2D\",\"\uD83C\uDFBE\",\"⛺\",\"\uD83E\uDDEA\",\"\uD83C\uDDF9\uD83C\uDDED\",\"\uD83C\uDF21\uFE0F\",\"\uD83E\uDD14\",\"\uD83E\uDE74\",\"\uD83D\uDCAD\",\"\uD83E\uDDF5\",\"3\uFE0F⃣\",\"\uD83C\uDFAB\",\"\uD83C\uDF9F\uFE0F\",\"\uD83D\uDC2F\",\"\uD83D\uDC05\",\"⏲\uFE0F\",\"\uD83C\uDDF9\uD83C\uDDF1\",\"\uD83D\uDE2B\",\"™\uFE0F\",\"\uD83C\uDDF9\uD83C\uDDEC\",\"\uD83D\uDEBD\",\"\uD83C\uDDF9\uD83C\uDDF0\",\"\uD83D\uDDFC\",\"\uD83C\uDF45\",\"\uD83C\uDDF9\uD83C\uDDF4\",\"\uD83D\uDC45\",\"\uD83E\uDDF0\",\"\uD83E\uDDB7\",\"\uD83E\uDEA5\",\"\uD83D\uDD1D\",\"\uD83C\uDFA9\",\"\uD83C\uDF2A\uFE0F\",\"\uD83C\uDDF9\uD83C\uDDF7\",\"\uD83D\uDDB2\uFE0F\",\"\uD83D\uDE9C\",\"\uD83D\uDEA5\",\"\uD83D\uDE8B\",\"\uD83D\uDE86\",\"\uD83D\uDE8A\",\"\uD83C\uDFF3\uFE0F\u200D⚧\uFE0F\",\"⚧\uFE0F\",\"\uD83D\uDEA9\",\"\uD83D\uDCD0\",\"\uD83D\uDD31\",\"\uD83C\uDDF9\uD83C\uDDF9\",\"\uD83C\uDDF9\uD83C\uDDE6\",\"\uD83D\uDE24\",\"\uD83E\uDDCC\",\"\uD83D\uDE8E\",\"\uD83C\uDFC6\",\"\uD83C\uDF79\",\"\uD83D\uDC20\",\"\uD83D\uDE9A\",\"\uD83C\uDFBA\",\"\uD83C\uDF37\",\"\uD83E\uDD43\",\"\uD83C\uDDF9\uD83C\uDDF3\",\"\uD83E\uDD83\",\"\uD83C\uDDF9\uD83C\uDDF2\",\"\uD83C\uDDF9\uD83C\uDDE8\",\"\uD83D\uDC22\",\"\uD83C\uDDF9\uD83C\uDDFB\",\"\uD83D\uDCFA\",\"\uD83D\uDD00\",\"2\uFE0F⃣\",\"\uD83D\uDC95\",\"\uD83D\uDC6C\",\"\uD83D\uDC6D\",\"\uD83C\uDE39\",\"\uD83C\uDE34\",\"\uD83C\uDE3A\",\"\uD83C\uDE2F\",\"\uD83C\uDE37\uFE0F\",\"\uD83C\uDE36\",\"\uD83C\uDE35\",\"\uD83C\uDE1A\",\"\uD83C\uDE38\",\"\uD83C\uDE32\",\"\uD83C\uDE33\",\"\uD83C\uDDFA\uD83C\uDDEC\",\"\uD83C\uDDFA\uD83C\uDDE6\",\"☔\",\"\uD83D\uDE12\",\"\uD83D\uDD1E\",\"\uD83E\uDD84\",\"\uD83C\uDDE6\uD83C\uDDEA\",\"\uD83C\uDDFA\uD83C\uDDF3\",\"\uD83D\uDD13\",\"\uD83C\uDD99\",\"\uD83D\uDE43\",\"\uD83C\uDDFA\uD83C\uDDFE\",\"\uD83C\uDDFA\uD83C\uDDF8\",\"\uD83C\uDDFA\uD83C\uDDF2\",\"\uD83C\uDDFB\uD83C\uDDEE\",\"\uD83C\uDDFA\uD83C\uDDFF\",\"✌\uFE0F\",\"\uD83E\uDDDB\",\"\uD83E\uDDDB\u200D♂\uFE0F\",\"\uD83E\uDDDB\u200D♀\uFE0F\",\"\uD83C\uDDFB\uD83C\uDDFA\",\"\uD83C\uDDFB\uD83C\uDDE6\",\"\uD83C\uDDFB\uD83C\uDDEA\",\"\uD83D\uDEA6\",\"\uD83D\uDCFC\",\"\uD83D\uDCF3\",\"\uD83D\uDCF9\",\"\uD83C\uDFAE\",\"\uD83C\uDDFB\uD83C\uDDF3\",\"\uD83C\uDFBB\",\"♍\",\"\uD83C\uDF0B\",\"\uD83C\uDFD0\",\"\uD83E\uDD2E\",\"\uD83C\uDD9A\",\"\uD83D\uDD96\",\"\uD83E\uDDC7\",\"\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC77\uDB40\uDC6C\uDB40\uDC73\uDB40\uDC7F\",\"\uD83D\uDEB6\",\"\uD83D\uDEB6\u200D♂\uFE0F\",\"\uD83D\uDEB6\u200D♀\uFE0F\",\"\uD83C\uDDFC\uD83C\uDDEB\",\"\uD83C\uDF18\",\"\uD83C\uDF16\",\"⚠\uFE0F\",\"\uD83D\uDDD1\uFE0F\",\"⌚\",\"\uD83D\uDC03\",\"\uD83E\uDD3D\",\"\uD83C\uDF49\",\"\uD83D\uDC4B\",\"〰\uFE0F\",\"\uD83C\uDF12\",\"\uD83D\uDEBE\",\"\uD83D\uDE29\",\"\uD83D\uDC92\",\"\uD83C\uDFCB\uFE0F\",\"\uD83C\uDFCB\uFE0F\u200D♂\uFE0F\",\"\uD83C\uDFCB\uFE0F\u200D♀\uFE0F\",\"\uD83C\uDDEA\uD83C\uDDED\",\"\uD83D\uDC33\",\"\uD83D\uDC0B\",\"\uD83D\uDEDE\",\"☸\uFE0F\",\"♿\",\"✅\",\"⚪\",\"\uD83C\uDFF3\uFE0F\",\"\uD83D\uDCAE\",\"\uD83D\uDC68\u200D\uD83E\uDDB3\",\"\uD83D\uDC69\u200D\uD83E\uDDB3\",\"\uD83E\uDD0D\",\"⬜\",\"◽\",\"◻\uFE0F\",\"▫\uFE0F\",\"\uD83D\uDD33\",\"\uD83E\uDD40\",\"\uD83C\uDF90\",\"\uD83C\uDF2C\uFE0F\",\"\uD83E\uDE9F\",\"\uD83C\uDF77\",\"\uD83E\uDEBD\",\"\uD83D\uDE09\",\"\uD83D\uDEDC\",\"\uD83D\uDC3A\",\"\uD83D\uDC69\",\"\uD83D\uDC69\u200D\uD83C\uDFA8\",\"\uD83D\uDC69\u200D\uD83D\uDE80\",\"\uD83E\uDDD4\u200D♀\uFE0F\",\"\uD83E\uDD38\u200D♀\uFE0F\",\"\uD83D\uDC69\u200D\uD83C\uDF73\",\"\uD83E\uDD26\u200D♀\uFE0F\",\"\uD83D\uDC69\u200D\uD83C\uDFED\",\"\uD83D\uDC69\u200D\uD83C\uDF3E\",\"\uD83D\uDC69\u200D\uD83C\uDF7C\",\"\uD83D\uDC69\u200D\uD83D\uDE92\",\"\uD83D\uDC69\u200D⚕\uFE0F\",\"\uD83D\uDC69\u200D\uD83E\uDDBD\",\"\uD83D\uDC69\u200D\uD83E\uDDBC\",\"\uD83E\uDD35\u200D♀\uFE0F\",\"\uD83D\uDC69\u200D⚖\uFE0F\",\"\uD83E\uDD39\u200D♀\uFE0F\",\"\uD83D\uDC69\u200D\uD83D\uDD27\",\"\uD83D\uDC69\u200D\uD83D\uDCBC\",\"\uD83D\uDC69\u200D✈\uFE0F\",\"\uD83E\uDD3E\u200D♀\uFE0F\",\"\uD83E\uDD3D\u200D♀\uFE0F\",\"\uD83D\uDC69\u200D\uD83D\uDD2C\",\"\uD83E\uDD37\u200D♀\uFE0F\",\"\uD83D\uDC69\u200D\uD83C\uDFA4\",\"\uD83D\uDC69\u200D\uD83C\uDF93\",\"\uD83D\uDC69\u200D\uD83C\uDFEB\",\"\uD83D\uDC69\u200D\uD83D\uDCBB\",\"\uD83E\uDDD5\",\"\uD83D\uDC69\u200D\uD83E\uDDAF\",\"\uD83D\uDC73\u200D♀\uFE0F\",\"\uD83D\uDC5A\",\"\uD83D\uDC52\",\"\uD83E\uDD3C\u200D♀\uFE0F\",\"\uD83D\uDEBA\",\"\uD83E\uDEB5\",\"\uD83E\uDD74\",\"\uD83D\uDDFA\uFE0F\",\"\uD83E\uDEB1\",\"\uD83D\uDE1F\",\"\uD83D\uDD27\",\"\uD83E\uDD3C\",\"✍\uFE0F\",\"❌\",\"\uD83E\uDE7B\",\"\uD83E\uDDF6\",\"\uD83E\uDD71\",\"\uD83D\uDFE1\",\"\uD83D\uDC9B\",\"\uD83D\uDFE8\",\"\uD83C\uDDFE\uD83C\uDDEA\",\"\uD83D\uDCB4\",\"☯\uFE0F\",\"\uD83E\uDE80\",\"\uD83D\uDE0B\",\"\uD83C\uDDFF\uD83C\uDDF2\",\"\uD83E\uDD2A\",\"⚡\",\"\uD83E\uDD93\",\"0\uFE0F⃣\",\"\uD83C\uDDFF\uD83C\uDDFC\",\"\uD83E\uDD10\",\"\uD83E\uDDDF\",\"\uD83E\uDDDF\u200D♂\uFE0F\",\"\uD83E\uDDDF\u200D♀\uFE0F\",\"\uD83D\uDCA4\"]"
    )


    if (emojisListGitHub.size != currentKeywordListGitHub.size) {
        error("emojisListGH.size != currentKeywordListGitHub.size")
    }

    return buildMap {
        for ((index, s) in emojisListGitHub.withIndex()) {
            putIfAbsent(s, mutableListOf(currentKeywordListGitHub[index]))?.add(currentKeywordListGitHub[index])
        }
    }
}

fun retrieveSlackEmojiShortcutsFile(): Map<String, List<String>> {
    val url =
        "https://app.slack.com/workspace-signin?redir=%2Fgantry%2Fauth%3Fapp%3Dclient%26lc%3D1724516944%26return_to%3D%252Fclient%26teams%3D"

    val webClient: WebClient = WebClient(BrowserVersion.FIREFOX)
    webClient.options.isJavaScriptEnabled = false // enable javascript
    webClient.options.isThrowExceptionOnScriptError = false //even if there is error in js continue
    webClient.options.isCssEnabled = false //even if there is error in js continue
    webClient.waitForBackgroundJavaScript(10000) // important! wait until javascript finishes rendering
    val page: HtmlPage = webClient.getPage(url)

    val scriptSrc = page.getByXPath<HtmlScript>("//script[@src]").stream()
        .map { it.srcAttribute }
        .filter { it.matches(Regex("https://a\\.slack-edge\\.com/.*/signin-core\\..*.primer.min.js")) }
        .findFirst()

    if (!scriptSrc.isPresent) {
        throw IllegalStateException("Emoji script source not found")
    }
    var jsContent = Jsoup.connect(scriptSrc.get())
        .ignoreContentType(true)
        .method(Connection.Method.GET)
        .maxBodySize(Integer.MAX_VALUE)
        .execute()
        .body()
    val start = ".exports=JSON.parse('{\"100\""
    jsContent = jsContent.substring(jsContent.indexOf(start) + 21)
    jsContent = jsContent.take(jsContent.indexOf("\"}}')},") + 3)
    val node = jacksonObjectMapper().readTree(jsContent)

    return buildMap<String, MutableList<String>> {
        node.properties().forEach {
            val key =
                it.value.get("unicode").asText().split("-")
                    .joinToString("") { str -> String(Character.toChars(str.toInt(16))) }
            if (containsKey(key)) {
                get(key)?.add(getStringWithColon(it.value.get("name").asText()))
            } else {
                put(key, mutableListOf(getStringWithColon(it.value.get("name").asText())))
            }

            if (it.value.hasNonNull("skinVariations")) {
                it.value.get("skinVariations").properties().forEach { variation ->
                    val skinVariationsKey = variation.value.get("unicode").asText().split("-")
                        .joinToString("") { str -> String(Character.toChars(str.toInt(16))) }
                    if (containsKey(skinVariationsKey)) {
                        get(skinVariationsKey)?.add(getStringWithColon(variation.value.get("name").asText()))
                    } else {
                        put(skinVariationsKey, mutableListOf(getStringWithColon(variation.value.get("name").asText())))
                    }
                }
            }
        }
    }
}

fun retrieveDiscordEmojiShortcutsFile(): Map<String, List<String>> {
    val url = "https://discord.com/channels/@me"

    val document = Jsoup.connect(url).userAgent("Mozilla").get()
    val scripts = document.select("script[src]")
    val src = scripts.stream().map { it.attr("src") }.filter { it.matches(Regex("/assets/web\\..*\\.js")) }.findFirst()
    if (!src.isPresent) {
        throw IllegalStateException("Emoji script source not found")
    }
    val response = Jsoup.connect("https://discord.com${src.get()}")
        .ignoreContentType(true)
        .method(Connection.Method.GET)
        .maxBodySize(Integer.MAX_VALUE)
        .execute()


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DiscordEmoji(
        val names: List<String>,
        val surrogates: String,
        val diversityChildren: List<Int>?,
        val hasDiversityParent: Boolean?,
        val diversity: List<String>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EmojiJson(
        val emojis: List<DiscordEmoji>,
        val nameToEmoji: Map<String, Int>,
    )


    val mainEmojiJsContentStart = "exports=JSON.parse('{\"emojis\":[{\""
    var mainEmojiJsContent = response.body()
    //File("discord.js").writeText(mainEmojiJsContent)

    mainEmojiJsContent =
        mainEmojiJsContent.substring(mainEmojiJsContent.indexOf(mainEmojiJsContentStart) + mainEmojiJsContentStart.length - 13)
    //mainEmojiJsContent = mainEmojiJsContent.substring(0, mainEmojiJsContent.indexOf("}]}')},") + 3)
    mainEmojiJsContent = mainEmojiJsContent.take(mainEmojiJsContent.indexOf("}')},") + 1)
    //File("discordmain.json").writeText(mainEmojiJsContent)


    val abbreviationsEmojiJsContentStart = "523558:function(e){\"use strict\";e.exports=JSON.parse('{\""
    var abbreviationsEmojiJsContent = response.body()
    abbreviationsEmojiJsContent = abbreviationsEmojiJsContent.substring(
        abbreviationsEmojiJsContent.indexOf(abbreviationsEmojiJsContentStart) + abbreviationsEmojiJsContentStart.length - 2
    )
    abbreviationsEmojiJsContent = abbreviationsEmojiJsContent.take(abbreviationsEmojiJsContent.indexOf("}')},") + 1)

    val abbreviationsEmojiNode =
        jacksonObjectMapper().readTree(abbreviationsEmojiJsContent.replace("\\\\", "\\").replace("\\'", "'"))

    //{">:(":"angry",">:-(":"angry",">=(":"angry",">=-(":"angry",":\\")":"blush",":-\\")":"blush"...}
    // Create a map {"angry": [">:(",">:-("...]...}
    val nameToShortcutMap = buildMap<String, MutableList<String>> {
        abbreviationsEmojiNode.properties().forEach {
            if (containsKey(it.value.asText())) {
                get(it.value.asText())?.add(it.key)
            } else {
                put(it.value.asText(), mutableListOf(it.key))
            }
        }
    }


    val emojiJsonNode = jacksonObjectMapper().readValue<EmojiJson>(
        mainEmojiJsContent.replace(
            Regex("\\\\x([0-9A-Fa-f]{2})"),
            "\\\\u00$1"
        )
    )

    val emojiIndexToNames = emojiJsonNode.nameToEmoji.entries.groupBy { it.value }

    //File("./emoji_source_files/discord_emoji_shortcuts.original.min.json").writeText(emojiArrayNode.toString())
    fun diversityToFitzPatrickAlias(diversity: String) = when (diversity) {
        "1f3fb" -> ":skin-tone-1:"
        "1f3fc" -> ":skin-tone-2:"
        "1f3fd" -> ":skin-tone-3:"
        "1f3fe" -> ":skin-tone-4:"
        "1f3ff" -> ":skin-tone-5:"
        else -> throw IllegalStateException(diversity)
    }

    return buildMap<String, MutableList<String>> {
        //Add the normal emoji aliases
        emojiJsonNode.emojis.forEachIndexed { index, emoji ->
            put(emoji.surrogates, emoji.names.map(::getStringWithColon).toMutableList())
            emojiIndexToNames[index]?.let {
                val names = it.map { it.key }.map(::getStringWithColon)
                get(emoji.surrogates)!!.addAll(names)
            }

            if (emoji.diversity?.size == 1) {
                emojiJsonNode.emojis.find { parentEmoji -> parentEmoji.diversityChildren?.contains(index) == true }
                    ?.let { parentEmoji ->
                        get(emoji.surrogates)!!.addAll(parentEmoji.names.map { name ->
                            getStringWithColon(name) + diversityToFitzPatrickAlias(
                                emoji.diversity[0]
                            )
                        })
                    }
            }

        }

        //Add the abbreviations like :) >:(
        nameToShortcutMap.forEach { entry ->
            emojiJsonNode.emojis.find { it.names.contains(entry.key) }
                ?.let { get(it.surrogates)!!.addAll(entry.value) }
        }
        /*
                //Add the emoji variants like dark and white color emojis
                emojiJsonNodeeeee.emojis.filter { it.diversityChildren != null }.forEach { discordEmoji ->
                            discordEmoji.diversityChildren!!.forEach { discordEmojiDiversityChildren ->
                                put(
                                    discordEmojiDiversityChildren.surrogates,
                                    discordEmojiDiversityChildren.names.map(::getStringWithColon).toMutableList()
                                )

                // Only emojis with 1 fitzpatrick modifier seem to be compatible with
                // :name::fitzpatrick-alias-1:
                // :name::fitzpatrick-alias-1::fitzpatrick-alias-2: does not work
                // Produces 2 separate emojis which will be combined by discord in the frontend?
                // Normal emoji by alias: 👨🏼‍❤️‍💋‍👨🏼 "\uD83D\uDC68\uD83C\uDFFC\u200D❤\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC68\uD83C\uDFFC"
                // Emoji by base name + fitzpatrick modifier (results in a small emoji): 👨‍❤️‍💋‍👨🏼 "\uD83D\uDC68\u200D❤\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC68\uD83C\uDFFC"
                                if (discordEmojiDiversityChildren.diversity.size == 1) {
                                    discordEmoji.names.map { discordEmojiName ->
                                        get(discordEmojiDiversityChildren.surrogates)!!.add(
                                            ":$discordEmojiName:" + discordEmojiDiversityChildren.diversity.joinToString(
                                                ""
                                            ) { diversityToFitzPatrickAlias(it) })
                                    }

                                }

                            }
                        }*/

    }
}

fun getStringWithColon(str: String) = ":$str:"

data class GradleEmoji(
    val emoji: String,
    val unicode: String,
    val htmlDec: String,
    val htmlHex: String,
    val urlEncoded: String,
    val discordAliases: Set<String>,
    val githubAliases: Set<String>,
    val slackAliases: Set<String>,
    var hasFitzpatrick: Boolean,
    var hasHairStyle: Boolean,
    val version: Double,
    val qualification: String,
    var description: Any,
    val group: String,
    val subgroup: String,
    val hasVariationSelectors: Boolean,
    var keywords: Any
)

val generatedSourcesDir = "${project(":jemoji").layout.buildDirectory.get()}/generated/jemoji"

//Using interfaces instead of classes because deep inheritance != good
/**
 * Startup task to generate the necessary source files for this project. Does not generate a new emojis.json.
 */
fun generateJavaSourceFiles() {
    val emojisPerInterface = 900
    //val emojisPerListInterface = 5000
    val emojiArrayNode: ArrayNode =
        jacksonObjectMapper().readTree(file(rootDir.absolutePath + "\\public\\emojis.json")) as ArrayNode

    createStaticConstantsClassFromPreComputation(emojiArrayNode)

    TypeSpec.interfaceBuilder("Emojis").apply {
        addModifiers(Modifier.PUBLIC)

        for (entry in emojiArrayNode.groupBy { it.get("subgroup").asText() }) {
            val emojiSubGroupInterfaceConstantVariables = mutableListOf<FieldSpec>()
            val emojiSubGroupInterfaceConstantVariablesValidNames = mutableListOf<FieldSpec>()
            entry.value.forEach {
                val constantName: String =
                    emojiDescriptionToConstantName(it.get("description").asText(), it.get("qualification").asText())

                val emojiConstantVariable = FieldSpec.builder(ClassName.get(Emoji::class.java), constantName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addJavadoc(it.get("emoji").asText())
                    .initializer(
                        CodeBlock.of(
                            $$"$T.getEmoji($S).orElseThrow($T::new)",
                            EmojiManager::class.java,
                            it.get("emoji").asText(),
                            java.lang.IllegalStateException::class.java
                        )
                    )
                    .build()

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
                        addSuperinterface(ClassName.get(jemojiBasePackagePathString, adjustedInterfaceName))
                        createSubGroupEmojiInterface(adjustedInterfaceName, it)
                    }
            } else {
                addSuperinterface(ClassName.get(jemojiBasePackagePathString, emojiSubgroupFileName))
                createSubGroupEmojiInterface(emojiSubgroupFileName, emojiSubGroupInterfaceConstantVariablesValidNames)
            }
        }
    }.saveGeneratedJavaSourceFile()
}

fun generateEmojiLanguageEnum(languages: List<String>) {
    TypeSpec.enumBuilder("EmojiLanguage").apply {
        addModifiers(Modifier.PUBLIC)
        languages.forEach {
            addEnumConstant(emojiGroupToEnumName(it), TypeSpec.anonymousClassBuilder($$"$S", it).build())
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
    }.saveGeneratedJavaSourceFile()
}

fun generateEmojiGroupEnum(groups: List<String>) {
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


    }.saveGeneratedJavaSourceFile()
}

fun generateEmojiSubGroupEnum(groups: List<Pair<String, String>>) {
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
    }.saveGeneratedJavaSourceFile()
}

fun createStaticConstantsClassFromPreComputation(emojiArrayNode: JsonNode) {
    TypeSpec.classBuilder("PreComputedConstants").apply {
        addModifiers(Modifier.FINAL)
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
                    emojiArrayNode.map { it.get("urlEncoded").asText() }.distinct()
                        .maxOfOrNull { it.codePointCount(0, it.length) }!!.toString()
                ).build()
        )

        addField(
            FieldSpec.builder(TypeName.INT, "MINIMUM_EMOJI_URL_ENCODED_LENGTH", *variablesModifiers.toTypedArray())
                .initializer(
                    $$"$L",
                    emojiArrayNode.map { it.get("urlEncoded").asText() }.distinct()
                        .minOfOrNull { it.codePointCount(0, it.length) }!!.toString()
                ).build()
        )

        addField(
            FieldSpec.builder(TypeName.INT, "ALIAS_EMOJI_MAX_LENGTH", *variablesModifiers.toTypedArray())
                .initializer(
                    $$"$L",
                    emojiArrayNode.flatMap { node ->
                        listOf("discordAliases", "githubAliases", "slackAliases").flatMap { key ->
                            node[key]?.map { it.asText() } ?: emptyList()
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
                emojiArrayNode.map { it.get("htmlDec").asText() }
                    .maxOfOrNull { it.chars().filter { ch: Int -> ch == ';'.code }.count() }!!.toInt()
            ).build()
        )

        addField(
            FieldSpec.builder(TypeName.INT, "MIN_HTML_DECIMAL_CODEPOINT_LENGTH", *variablesModifiers.toTypedArray())
                .initializer(
                    $$"$L",
                    emojiArrayNode.map { it.get("htmlDec").asText() }
                        .minOfOrNull { text: String -> text.codePoints().toArray().size }!!.toString()
                ).build()
        )

        emojiArrayNode.asSequence()
            .flatMap { node ->
                listOf("discordAliases", "githubAliases", "slackAliases").flatMap { key ->
                    node[key]?.map { it.asText() } ?: emptyList()
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

        emojiArrayNode.asSequence().map { it.get("urlEncoded").asText() }
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

        emojiArrayNode.asSequence().map { it.get("urlEncoded").asText() }
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
    }.saveGeneratedJavaSourceFile()
}

fun createSubGroupEmojiInterface(
    emojiSubgroupFileName: String,
    emojiSubGroupInterfaceConstantVariables: List<FieldSpec>
) {
    TypeSpec.interfaceBuilder(emojiSubgroupFileName).apply {
        addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", $$"$S", "unused").build())
        addFields(emojiSubGroupInterfaceConstantVariables)
    }.saveGeneratedJavaSourceFile()
}

fun TypeSpec.Builder.saveGeneratedJavaSourceFile() {
    addAnnotation(
        AnnotationSpec.builder(ClassName.get("javax.annotation", "Generated"))
            .addMember("value", $$"$S", "build.gradle.kts")
            .build()
    )
    JavaFile.builder(jemojiPackagePath.joinToString("."), this.build())
        .indent("    ")
        .skipJavaLangImports(true)
        .build()
        .writeTo(file(generatedSourcesDir).toPath())
}


fun getParameterizedTypName(clazz: Class<*>, clazz2: Class<*>): ParameterizedTypeName =
    ParameterizedTypeName.get(ClassName.get(clazz), ClassName.get(clazz2))

fun getParameterizedTypName(clazz: Class<*>, packageName: String, simpleName: String): ParameterizedTypeName =
    ParameterizedTypeName.get(ClassName.get(clazz), ClassName.get(packageName, simpleName))

// Fix quotation marks
val replaceQuotationMarksRegex = Regex("\"")
// Fix error with aliases like: :-\
val replaceBackslashRegex = Regex("[\\\\]")

// FIX DESCRIPTION ERRORS THAT CAUSE COMPILATION ERRORS WITH NAMES
// Basic allowed characters
val descriptionReplaceRegex = Regex("[^A-Z0-9_]+")
// Fix error with emoji names like Keycap: *
val descriptionReplaceStarRegex = Regex("\\*")
// Fix error with emoji names like Keycap: #
val descriptionReplaceHashRegex = Regex("#")
// Fix error with emoji names like: A button (blood type)
// which results in an ending _ in the name
val descriptionReplaceEndingUnderscoreRegex = Regex("_$")

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
        .split(" ").joinToString("") { it.uppercaseFirstChar() }
        .split("-").joinToString("") { it.uppercaseFirstChar() }
        .split("&")
        .joinToString("And") { it.replaceFirstChar(Char::uppercaseChar) }
}

fun emojiGroupToEnumName(group: String): String {
    return group.uppercase().replace("-", "_").replace("&", "AND").replace(" ", "_").replace("__", "_")
}
