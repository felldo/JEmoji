import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.EnumConstantDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.*
import net.fellbaum.jemoji.Fitzpatrick
import net.fellbaum.jemoji.HairStyle
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.toHexString
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.util.stream.Collectors
import kotlin.math.ceil

plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    //https://github.com/melix/jmh-gradle-plugin
    id("me.champeau.jmh") version "0.7.2"
}


val java9: SourceSet by sourceSets.creating

tasks.named<JavaCompile>(java9.compileJavaTaskName) {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(9)
    }
    options.compilerArgumentProviders.add(object : CommandLineArgumentProvider {
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val mainClassesDirs = sourceSets.main.map { it.output.classesDirs }

        override fun asArguments() = mainClassesDirs
            .get()
            .files
            .map { it.absolutePath }
            .flatMap {
                listOf(
                    "--patch-module",
                    "net.fellbaum.jemoji=$it"
                )
            }
    })
}

tasks.jar {
    manifest {
        attributes("Multi-Release" to true)
    }
    from(java9.output) {
        into("META-INF/versions/9/")
    }
}

val java9Implementation by configurations.existing {
    extendsFrom(configurations.implementation.get())
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi("org.jspecify:jspecify:0.3.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit4 test framework
            useJUnit("4.13.2")
        }
    }
}

jmh {
//    includes.addAll("some regular expression")  // include pattern (regular expression) for benchmarks to be executed
    excludes.addAll("excluded") // exclude pattern (regular expression) for benchmarks to be executed
//    iterations.set(10) // Number of measurement iterations to do.
    benchmarkMode.addAll("avgt") // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
//    batchSize.set(1) // Batch size: number of benchmark method calls per operation. (some benchmark modes can ignore this setting)
    fork.set(2) // How many times to forks a single benchmark. Use 0 to disable forking altogether
//    failOnError.set(false) // Should JMH fail immediately if any benchmark had experienced the unrecoverable error?
//    forceGC.set(false) // Should JMH force GC between iterations?
//    jvm.set("myjvm") // Custom JVM to use when forking.
//    jvmArgs.addAll("Custom JVM args to use when forking.")
//    jvmArgsAppend.addAll("Custom JVM args to use when forking (append these)")
//    jvmArgsPrepend.addAll("Custom JVM args to use when forking (prepend these)")
//    humanOutputFile.set(project.file("${project.buildDir}/reports/jmh/human.txt")) // human-readable output file
//    resultsFile.set(project.file("${project.buildDir}/reports/jmh/results.txt")) // results file
//    operationsPerInvocation.set(10) // Operations per invocation.
//    //benchmarkParameters.put("", ListProperty) // Benchmark parameters.
//    profilers.addAll("") // Use profilers to collect additional data. Supported profilers: [cl, comp, gc, stack, perf, perfnorm, perfasm, xperf, xperfasm, hs_cl, hs_comp, hs_gc, hs_rt, hs_thr, async]
//    timeOnIteration.set("1s") // Time to spend at each measurement iteration.
    resultFormat.set("JSON") // Result format type (one of CSV, JSON, NONE, SCSV, TEXT)
//    synchronizeIterations.set(false) // Synchronize iterations?
//    threads.set(4) // Number of worker threads to run with.
//    threadGroups.addAll(2,3,4) //Override thread group distribution for asymmetric benchmarks.
//    jmhTimeout.set("1s") // Timeout for benchmark iteration.
//    timeUnit.set("ms") // Output time unit. Available time units are: [m, s, ms, us, ns].
//    verbosity.set("NORMAL") // Verbosity mode. Available modes are: [SILENT, NORMAL, EXTRA]
//    warmup.set("1s") // Time to spend at each warmup iteration.
//    warmupBatchSize.set(10) // Warmup batch size: number of benchmark method calls per operation.
//    warmupForks.set(0) // How many warmup forks to make for a single benchmark. 0 to disable warmup forks.
//    warmupIterations.set(1) // Number of warmup iterations to do.
//    warmupMode.set("INDI") // Warmup mode for warming up selected benchmarks. Warmup modes are: [INDI, BULK, BULK_INDI].
//    warmupBenchmarks.addAll(".*Warmup") // Warmup benchmarks to include in the run in addition to already selected. JMH will not measure these benchmarks, but only use them for the warmup.
//
//    zip64.set(true) // Use ZIP64 format for bigger archives
//    jmhVersion.set("1.36") // Specifies JMH version
//    includeTests.set(true) // Allows to include test sources into generate JMH jar, i.e. use it when benchmarks depend on the test classes.
//    duplicateClassesStrategy.set(DuplicatesStrategy.FAIL) // Strategy to apply when encountring duplicate classes during creation of the fat jar (i.e. while executing jmhJar task)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    systemProperty("file.encoding", "UTF-8")
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

//Generate sources before compiling if they do not exist
tasks.named("compileJava") {
    if (!file("./build/generated/jemoji/net/fellbaum/jemoji/Emojis.java").exists()) {
        dependsOn("generateJavaSourceFiles")
    }
    if (!file("./build/generated/jemoji/net/fellbaum/jemoji/EmojiDescriptionLanguage.java").exists()) {
        dependsOn("generateEmojisDescription")
    }
}

tasks.named("build") {
    finalizedBy("copyJarToProject")
}

/**
 * Startup task to generate the needed source files for this project. Does not generate a new emojis.json.
 */
tasks.register("generate") {
    dependsOn("generateEmojisDescription")
    dependsOn("generateJavaSourceFiles")
}

tasks.register("copyJarToProject") {
    doLast {
        copy {
            from("${layout.buildDirectory}/libs/jemoji.jar")
            into(project.rootDir.path + "\\libs")
        }
    }
}

publishing {
    if (project.gradle.startParameter.taskNames.contains("publish")
        or project.gradle.startParameter.taskNames.contains("publishToMavenLocal")
    ) {
        project.version = project.version.toString().replace("-SNAPSHOT", "")
    }
    publications {
        create<MavenPublication>("JEMOJI") {
            artifactId = "jemoji"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("Java Emoji")
                description.set(rootProject.description)
                url.set("https://github.com/felldo/JEmoji")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                inceptionYear.set("2023")
                developers {
                    developer {
                        id.set("felldo")
                        name.set("Dominic Fellbaum")
                        email.set("d.fellbaum@hotmail.de")
                        url.set("https://github.com/felldo")
                        timezone.set("Europe/Berlin")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/felldo/JEmoji.git")
                    developerConnection.set("scm:git:git@github.com:felldo/JEmoji.git")
                    url.set("https://github.com/felldo/JEmoji")
                }
            }
        }
    }
    repositories {
        maven {
            val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")
            name = "OSSRH"
            url = if (isReleaseVersion) {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            } else {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            }
            credentials {
                username = findPropertyOrNull("NEXUS_USERNAME")
                password = findPropertyOrNull("NEXUS_PASSWORD")
            }
        }
    }
}

fun findPropertyOrNull(name: String) = if (hasProperty(name)) project.property(name) as String else null

signing {
    val signingKey = findPropertyOrNull("JEMOJI_SINGING_SECRET_KEY_RING_FILE")
    val signingKeyId = findPropertyOrNull("JEMOJI_SIGNING_KEY_ID")
    val signingPassword = findPropertyOrNull("JEMOJI_SIGNING_PASSWORD")
    isRequired = !signingKey.isNullOrBlank()

    if (project.gradle.startParameter.taskNames.any { anyTask ->
            tasks.mapNotNull { if (it.group == "publishing") it.name else null }.contains(anyTask)
        }) {
        println("Executing a publishing task. The jar will be signed: $isRequired")
    }

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["JEMOJI"])
}

val prePublishTask by tasks.register("prePublishTask") {
    doFirst {
        if (findPropertyOrNull("JEMOJI_SINGING_SECRET_KEY_RING_FILE").isNullOrBlank()) {
            throw Exception("Can not publish a new release because secrets are missing for singing")
        }
    }
}

// When publishing, check if the secrets are available
tasks.named("publish") {
    dependsOn(prePublishTask)
}

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        locale = "en"
        encoding = "UTF-8"
        docTitle = "JEmoji ${project.version}"
        windowTitle = "$docTitle Documentation"
        links("https://docs.oracle.com/javase/8/docs/api/")
        isUse = true
        isVersion = true
        isAuthor = true
        isSplitIndex = true

        val toolchain = javadocTool
            .map { JavaVersion.toVersion(it.metadata.languageVersion) }
            .orElse(provider { JavaVersion.current() })
            .get()
        if (toolchain.isCompatibleWith(JavaVersion.VERSION_1_9)) {
            addBooleanOption("html5", true)
            addStringOption("-release", java.targetCompatibility.majorVersion)
            if (toolchain.isCompatibleWith(JavaVersion.VERSION_11) && !toolchain.isCompatibleWith(JavaVersion.VERSION_13)) {
                addBooleanOption("-no-module-directories", true)
            }
        } else {
            source = java.sourceCompatibility.toString()
        }
    }
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.fasterxml.jackson.core:jackson-databind:2.16.1")
        classpath("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
        classpath("com.squareup.okhttp3:okhttp:4.9.3")

        classpath("org.jsoup:jsoup:1.17.2")
        classpath("com.github.javaparser:javaparser-symbol-solver-core:3.25.9")
        classpath(files(project.rootDir.path + "\\libs\\jemoji.jar"))
    }
}
// https://github.com/unicode-org/cldr/tree/main/common/annotations
// https://github.com/unicode-org/cldr-json/tree/main/cldr-json
// https://stackoverflow.com/questions/39490865/how-can-i-get-the-full-list-of-slack-emoji-through-api
// https://github.com/iamcal/emoji-data/
tasks.register("generateEmojisDescription") {
    val objectMapper = ObjectMapper()
    val client = OkHttpClient()
    val repo = "unicode-org/cldr-json"

    val httpBuilderAnnotationsDerivedDirectory: HttpUrl.Builder =
        "https://api.github.com/repos/${repo}/contents/cldr-json/cldr-annotations-derived-full/annotationsDerived".toHttpUrl()
            .newBuilder()
    val requestBuilder: Request.Builder = Request.Builder().addHeader("Accept", "application/vnd.github.raw+json")
    requestBuilder.url(httpBuilderAnnotationsDerivedDirectory.build())
    val descriptionDirectory: JsonNode =
        objectMapper.readTree(client.newCall(requestBuilder.build()).execute().body!!.string())

    val fileNameList = mutableListOf<String>()

    for (directory in descriptionDirectory) {
        val descriptionNodeOutput = JsonNodeFactory.instance.objectNode()

        requestCLDREmojiDescriptionTranslation(
            "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/cldr-annotations-derived-full/annotationsDerived/${
                directory.get(
                    "name"
                ).asText()
            }/annotations.json", client, objectMapper, descriptionNodeOutput, directory.get("name").asText()
        )

        requestCLDREmojiDescriptionTranslation(
            "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/cldr-annotations-full/annotations/${
                directory.get(
                    "name"
                ).asText()
            }/annotations.json", client, objectMapper, descriptionNodeOutput, directory.get("name").asText()
        )


        val descriptionFile =
            File("$projectDir/src/main/resources/emoji_sources/description/${directory.get("name").asText()}.json")
        descriptionFile.writeText(objectMapper.writeValueAsString(descriptionNodeOutput))
        fileNameList.add(directory.get("name").asText())
    }

    generateEmojiDescriptionLanguageEnum(fileNameList)
}
fun requestCLDREmojiDescriptionTranslation(
    url: String,
    client: OkHttpClient,
    mapper: ObjectMapper,
    descriptionNodeOutput: ObjectNode,
    fileName: String
) {
    val fileHttpBuilder: HttpUrl.Builder = url.toHttpUrl().newBuilder()
    val requestBuilder: Request.Builder = Request.Builder().addHeader("Accept", "application/vnd.github.raw+json")
    requestBuilder.url(fileHttpBuilder.build())

    val fileContent: JsonNode = mapper.readTree(client.newCall(requestBuilder.build()).execute().body!!.string())

    val translationFile = File("$rootDir/emoji_source_files/description/$fileName.json")
    translationFile.writeText(mapper.writeValueAsString(fileContent))

    val annotationsDerived =
        if (fileContent.has("annotations")) fileContent.get("annotations") else fileContent.get("annotationsDerived")
    if (!annotationsDerived.has("annotations")) {
        return
    }

    val annotationsNode = annotationsDerived.get("annotations")


    annotationsNode.fields().forEach {
        if (it.value.has("tts")) {
            descriptionNodeOutput.put(it.key, it.value.get("tts").joinToString(" ") { it.asText() })
        }
    }
}

val jemojiPackagePath = listOf("net", "fellbaum", "jemoji")
fun generateEmojiDescriptionLanguageEnum(languages: List<String>) {
    val fileName = "EmojiDescriptionLanguage"

    val emojisPath = "${jemojiPackagePath.joinToString("/")}/$fileName.java"
    val compilationUnit = CompilationUnit(jemojiPackagePath.joinToString("."))

    compilationUnit.setStorage(file("$generatedSourcesDir/$emojisPath").toPath())
    val enumFile = compilationUnit.addEnum(fileName)

    val nodeList = NodeList<EnumConstantDeclaration>()
    languages.forEach {
        val constantName = emojiGroupToEnumName(it)
        val enumConstantDeclaration = EnumConstantDeclaration(constantName).addArgument(StringLiteralExpr(it))
        nodeList.add(enumConstantDeclaration)
    }

    enumFile.setEntries(nodeList)

    val valueField = enumFile.addPrivateField(String::class.java, "value").setFinal(true)
    val enumConstructor = enumFile.addConstructor()

    enumConstructor.addParameter(String::class.java, "value")
    enumConstructor.createBody().addStatement("this.value = value;")
    valueField.createGetter().setJavadocComment(
        """
                Gets the value.

                @return The value.
            """.trimIndent()
    )

    compilationUnit.storage.get().save()
}

tasks.register("generateEmojis") {
    //dependsOn("build")
    doLast {

        val unicodeTestDataUrl = "https://unicode.org/Public/emoji/latest/emoji-test.txt"
        val unicodeVariationSequences = "https://www.unicode.org/Public/UCD/latest/ucd/emoji/emoji-variation-sequences.txt"

        val client = OkHttpClient()
        val mapper = jacksonObjectMapper()

        val githubEmojiAliasMap = getGithubEmojiAliasMap(client, mapper)
        val emojiTerraMap = getEmojiTerraMap()
        val discordAliases = getDiscordAliasMap(client, mapper)

        val githubEmojiDefinition = File("$rootDir/emoji_source_files/github-emoji-definition.json")
        githubEmojiDefinition.writeText(mapper.writeValueAsString(githubEmojiAliasMap))
        val emojiTerraEmojiDefinition = File("$rootDir/emoji_source_files/emojiterra-emoji-definition.json")
        emojiTerraEmojiDefinition.writeText(mapper.writeValueAsString(emojiTerraMap))
        val discordEmojiDefinition = File("$rootDir/emoji_source_files/discord-emoji-definition.json")
        discordEmojiDefinition.writeText(mapper.writeValueAsString(discordAliases))

        val unicodeVariationLines = client.newCall(Request.Builder().url(unicodeVariationSequences).build()).execute().body!!.string()

        val emojisThatHaveVariations = unicodeVariationLines.lines()
            .asSequence()
            .filter { !it.startsWith("#") }
            .filter { it.isNotEmpty() }
            .map { it.split(";") }
            .map { it[0].split(" ")[0].trim() }
            .distinct()
            .map { String(Character.toChars(it.toInt(16))) }
            .toSet()

        val unicodeLines = client.newCall(Request.Builder().url(unicodeTestDataUrl).build()).execute().body!!.string()

        // drop the first block as it's just the header
        val allUnicodeEmojis = unicodeLines.split("# group: ").drop(1).flatMap { group ->
            /*
                "group" is a string containing everything from a group which looks like:
                # group: Smileys & Emotion
                # subgroup: face-smiling
                1F600     ; fully-qualified     # 😀 E0.6 grinning face

                # subgroup: face-affection
                1F970     ; fully-qualified     # 🥰 E11.0 smiling face with hearts
             */

            /*
                "groupSplit" is a list containing the group name and all subgroups
                [0] = group name
                [1] = subgroup 1
                [2] = subgroup 2
            */
            val groupSplit = group.split("# subgroup: ")

            // Get the first line of the group which is the group name and ignore the rest as they are just empty lines
            val groupName = groupSplit[0].lines()[0]

            groupSplit.drop(1).flatMap { subGroup ->
                /*
                    "subGroup" is a string containing everything from a subgroup which looks like:
                    face-smiling
                    1F600     ; fully-qualified     # 😀 E0.6 grinning face

                    face-affection
                    1F970     ; fully-qualified     # 🥰 E11.0 smiling face with hearts
                 */
                val subGroupLines = subGroup.lines()
                val subGroupName = subGroupLines[0]
                //println(subGroupName)
                subGroupLines.asSequence().drop(1)
                    .filter { !it.startsWith("#") && it.isNotBlank() }
                    .map { it.split(";") }
                    .map { stringList ->
                        // 1F44D     ; fully-qualified     # 👍 E0.6 thumbs up
                        //[   [0]    ][                [1]                   ]

                        val cpOrigString = stringList[0].trim().replace(" ", "-")

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
                        val emojiTerraInfo = emojiTerraMap[codepointsString]

                        val charsAsString = codepointsString.chars()
                            .mapToObj { "\\u" + it.toHexString().uppercase().padStart(4, '0') }
                            .collect(Collectors.joining(""))


                        val aliasWrapper = AliasInfo(
                            emojiTerraInfo,
                            discordAliases,
                            githubEmojiAliasMap,
                            cpOrigString,
                            codepointsString
                        )

                        val completeDiscordAliases = aliasWrapper.getAllDiscordAliases()
                        val completeGitHubAliases = aliasWrapper.getAllGithubAliases()
                        val completeSlackAliases = aliasWrapper.getAllSlackAliases()

                        Emoji(
                            codepointsString,
                            //Get each char and fill with leading 0 as the representation is: \u0000
                            "\"$charsAsString\"",
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
                            emojisThatHaveVariations.contains(codepointsString)
                        )
                    }.toList()
            }
        }

//        val overrideEmojisFile =
//            File("$projectDir/src/main/resources/emojis-override.json") //TODO: Allow specific overrides or additions to i.e. aliases
//        val overrideEmojisJson = mapper.readTree(overrideEmojisFile.readText())

        /*for (jsonNode in overrideEmojisJson) {
            Emoji()
        }*/

        //val resourceFile = File("$projectDir/src/main/resources/emojis.json")
        val publicFile = File("$rootDir/public/emojis.json")
        val publicFileMin = File("$rootDir/public/emojis.min.json")

        //resourceFile.writeText(mapper.writeValueAsString(allUnicodeEmojis))
        publicFile.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allUnicodeEmojis))
        publicFileMin.writeText(mapper.writeValueAsString(allUnicodeEmojis))
    }
}

class AliasInfo(
    private val emojiTerraInfo: EmojiTerraInfo?,
    private val discordAliases: Map<String, List<String>>,
    private val githubEmojiAliasMap: Map<String, List<Pair<String, String>>>,
    private val utf16CodePointString: String,
    private val emojiString: String
) {

    fun getAllGithubAliases(): Set<String> = buildSet {
        githubEmojiAliasMap[utf16CodePointString]?.let { pairList ->
            addAll(pairList.map { it.first }.toList())
        }
        emojiTerraInfo?.githubCode?.let { add(it) }
    }

    fun getAllSlackAliases(): Set<String> = buildSet {
        emojiTerraInfo?.slackCode?.let { add(it) }
    }

    fun getAllDiscordAliases(): Set<String> = buildSet {
        discordAliases[emojiString]?.let { addAll(it) }
        emojiTerraInfo?.discordCode?.let { add(it) }
    }
}

data class EmojiTerraInfo(
    val discordCode: String?,
    val githubCode: String?,
    val slackCode: String?,
    val keywords: List<String>
)

fun fetchEmojiTerra(url: String): Connection.Response {
    val emojiTerraBaseUrl = "https://emojiterra.com"
    return Jsoup.connect(emojiTerraBaseUrl + url)
        .userAgent("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.121 Safari/535.2")
        .header("Connection", "keep-alive")
        .header("Cache-Control", "max-age=0")
        .header("Upgrade-Insecure-Requests", "1")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        .timeout(10000)
        .ignoreHttpErrors(true)
        .execute()
}

fun getGithubEmojiAliasMap(client: OkHttpClient, mapper: ObjectMapper): Map<String, List<Pair<String, String>>> {
    val gitHubEmojiAliasUrl = "https://api.github.com/emojis"

    val json = client.newCall(Request.Builder().url(gitHubEmojiAliasUrl).build()).execute().body!!.string()

    return mapper.readTree(json)
        .fields()
        .asSequence()
        .map { it.key to it.value.asText().uppercase() }
        .filter {
            it.second.contains(
                "https://github.githubassets.com/images/icons/emoji/unicode",
                ignoreCase = true
            )
        }
        .map {
            ":${it.first}:" to it.second.replace(
                "https://github.githubassets.com/images/icons/emoji/unicode/",
                "",
                true
            ).replace(".png?v8", "", true)
        }
        .groupBy { it.second }
    //{1F44D=[(+1, 1F44D), (thumbsup, 1F44D)]
}

fun getEmojiTerraMap(): Map<String, EmojiTerraInfo> {
    val emojiTerraUrl = "https://emojiterra.com/list/"

    val list = Jsoup.connect(emojiTerraUrl)
        .userAgent("Mozilla")
        .timeout(5000)
        .get()

    return buildMap {
        list.select("tbody > tr > td > a")
            .mapIndexed { index, listElement ->

                if (index % 10 == 0) println(index)

                var response: Connection.Response
                do {
                    response = fetchEmojiTerra(listElement.attr("href"))
                    if (response.statusCode() != 200) {
                        Thread.sleep(500)
                        println("RETRYING: ${response.statusCode()} - ${response.statusMessage()}")
                    }
                } while (response.statusCode() != 200)

                val document = response.parse()
                val codes = document.getElementsByClass("codes-bold")
                val discordCode = codes.firstOrNull { it.id().contains("discord") }?.text()
                val githubCode = codes.firstOrNull { it.id().contains("github") }?.text()
                val slackCode = codes.firstOrNull { it.id().contains("slack") }?.text()
                val keywords = document.getElementById("annotations-keywords")
                    ?.text()
                    ?.replace("Keywords:", "")
                    ?.split("|")
                    ?.map { it.trim() }
                    ?: emptyList()

                Pair(
                    document.getElementById("copy-emoji")?.ownText()!!,
                    EmojiTerraInfo(discordCode, githubCode, slackCode, keywords)
                )
            }.forEach { put(it.first, it.second) }
    }

}

data class Emoji(
    val emoji: String,
    @JsonRawValue val unicode: String,
    val discordAliases: Set<String>,
    val githubAliases: Set<String>,
    val slackAliases: Set<String>,
    var hasFitzpatrick: Boolean,
    var hasHairStyle: Boolean,
    val version: Double,
    val qualification: String,
    val description: String,
    val group: String,
    val subgroup: String,
    val hasVariationSelectors: Boolean
)

fun getDiscordAliasMap(client: OkHttpClient, mapper: ObjectMapper): Map<String, List<String>> {
    val discordEmojiAliasUrl = "https://emzi0767.gl-pages.emzi0767.dev/discord-emoji/discordEmojiMap.json"

    val json =
        mapper.readTree(client.newCall(Request.Builder().url(discordEmojiAliasUrl).build()).execute().body!!.string())

    return buildMap {
        for (jsonNode in json.get("emojiDefinitions")) {
            val aliases = buildList {
                jsonNode.get("namesWithColons").forEach { add(it.asText()) }
            }

            val key = jsonNode.get("surrogates").asText()

            put(key, aliases)
        }
    }
}

val generatedSourcesDir = "$buildDir/generated/jemoji"
sourceSets {
    main {
        java.setSrcDirs(java.srcDirs + files(generatedSourcesDir))
    }
}

//Using interfaces instead of classes because deep inheritance != good
tasks.register("generateJavaSourceFiles") {
    doLast {
        val emojisPerInterface = 500
        val emojisPerListInterface = 5000
        val emojiArrayNode =
            jacksonObjectMapper().readTree(file(rootDir.absolutePath + "\\public\\emojis.json"))

        val emojisCompilationUnit = CompilationUnit(jemojiPackagePath.joinToString("."))
        emojisCompilationUnit.setStorage(file("$generatedSourcesDir/${jemojiPackagePath.joinToString("/")}/Emojis.java").toPath())
        val emojisInterface = emojisCompilationUnit.addInterface("Emojis")
        emojisInterface.setPublic(true)
        emojisCompilationUnit.run {
            addImport("java.util.Arrays")
            addImport("java.util.List")
        }

        val emojisListField: FieldDeclaration = emojisInterface.addField("List<Emoji>", "EMOJI_LIST")
        val emojisArrayCreationExpr = MethodCallExpr(NameExpr("Arrays"), "asList")

        val emojiClassType = JavaParser().parseClassOrInterfaceType("Emoji").result.get()
        val emojiFileNameToConstants = mutableMapOf<String, List<FieldDeclaration>>()
        for (entry in emojiArrayNode.groupBy { it.get("subgroup").asText() }) {
            val emojiSubGroupInterfaceConstantVariables = NodeList<FieldDeclaration>()
            entry.value.forEach {
                val constantName: String =
                    emojiDescriptionToConstantName(it.get("description").asText(), it.get("qualification").asText())

                val emojiConstantVariable = FieldDeclaration()
                    .addVariable(VariableDeclarator(emojiClassType, constantName))
                    .setJavadocComment(JavadocComment(it.get("emoji").asText()))

                emojiSubGroupInterfaceConstantVariables.add(emojiConstantVariable)

                val discordAliases = getAndSanitizeEmojiAliases(it.get("discordAliases"))
                val slackAliases = getAndSanitizeEmojiAliases(it.get("slackAliases"))
                val githubAliases = getAndSanitizeEmojiAliases(it.get("githubAliases"))
                val qualification = it.get("qualification").asText()

                val initializer = ObjectCreationExpr().apply {
                    setType(emojiClassType)
                    addArgument(StringLiteralExpr(it.get("emoji").asText()))
                    addArgument(StringLiteralExpr(it.get("unicode").asText().chars()
                        .mapToObj { "\\u" + it.toHexString().uppercase().padStart(4, '0') }
                        .collect(Collectors.joining(""))))
                    addArgument(getGeneratedMethodCallExprForEntries(discordAliases))
                    addArgument(getGeneratedMethodCallExprForEntries(slackAliases))
                    addArgument(getGeneratedMethodCallExprForEntries(githubAliases))
                    addArgument(BooleanLiteralExpr(it.get("hasFitzpatrick").asBoolean()))
                    addArgument(BooleanLiteralExpr(it.get("hasHairStyle").asBoolean()))
                    addArgument(DoubleLiteralExpr(it.get("version").asDouble()))
                    addArgument(MethodCallExpr("Qualification.fromString").addArgument(StringLiteralExpr(qualification)))
                    addArgument(StringLiteralExpr(it.get("description").asText()))
                    addArgument(NameExpr("EmojiGroup." + emojiGroupToEnumName(it.get("group").asText())))
                    addArgument(NameExpr("EmojiSubGroup." + emojiGroupToEnumName(it.get("subgroup").asText())))
                    addArgument(BooleanLiteralExpr(it.get("hasVariationSelectors").asBoolean()))
                }
                emojiConstantVariable.getVariable(0).setInitializer(initializer)
            }

            emojiSubGroupInterfaceConstantVariables.groupBy { it.getVariable(0).name }.onEach {
                if (it.value.size > 1) {
                    it.value.forEachIndexed { index, enumConstantDeclaration ->
                        enumConstantDeclaration.getVariable(0)
                            .setName(enumConstantDeclaration.getVariable(0).name.asString() + "_$index")
                    }
                }
            }

            // After changing duplicated names of some emojis, add them to the all emojis list
            emojiSubGroupInterfaceConstantVariables.forEach {
                emojisArrayCreationExpr.addArgument(it.getVariable(0).name.toString())
            }

            // Create multiple interfaces of the same SubGroup, if there are more than X emojis
            val emojiSubgroupFileName = emojiDescriptionToFileName(entry.key)
            if (ceil(emojiSubGroupInterfaceConstantVariables.size / emojisPerInterface.toDouble()) > 1) {
                var startingLetter = 'A'
                emojiSubGroupInterfaceConstantVariables.windowed(emojisPerInterface, emojisPerInterface, true).forEach {
                    val adjustedInterfaceName = emojiSubgroupFileName + (startingLetter++)
                    emojiFileNameToConstants[adjustedInterfaceName] = it
                    emojisInterface.addExtendedType(adjustedInterfaceName)
                    createSubGroupEmojiInterface(jemojiPackagePath, adjustedInterfaceName, it)
                }
            } else {
                emojiFileNameToConstants[emojiSubgroupFileName] = emojiSubGroupInterfaceConstantVariables
                emojisInterface.addExtendedType(emojiSubgroupFileName)
                createSubGroupEmojiInterface(
                    jemojiPackagePath,
                    emojiSubgroupFileName,
                    emojiSubGroupInterfaceConstantVariables
                )
            }
        }

        val mapEntriesListWithNoMoreThanXEntries = mutableListOf<MutableList<Pair<String, List<FieldDeclaration>>>>()
        emojiFileNameToConstants.entries.forEach {
            var added = false

            for (list in mapEntriesListWithNoMoreThanXEntries) {
                if (list.sumOf { it.second.size } + it.value.size <= emojisPerListInterface) {
                    list.add(Pair(it.key, it.value))
                    added = true
                    break
                }
            }
            if (!added) {
                mapEntriesListWithNoMoreThanXEntries.add(mutableListOf(Pair(it.key, it.value)))
            }
        }

        //Create the emoji loader interfaces grouped by max X entries per interface
        var startingLetter = 'A'
        mapEntriesListWithNoMoreThanXEntries.forEach {
            createEmojiLoaderInterface(jemojiPackagePath, "EmojiLoader" + (startingLetter++), it)
        }

        emojisListField.getVariable(0).setInitializer(emojisArrayCreationExpr)
        emojisCompilationUnit.storage.get().save()
    }
}


fun createEmojiLoaderInterface(
    path: List<String>,
    filename: String,
    emojiSubGroupInterfaceConstantVariables: MutableList<Pair<String, List<FieldDeclaration>>>
) {
    val emojiSubgroupFilePath = "${path.joinToString("/")}/$filename.java"
    val emojiSubGroupCompilationUnit = CompilationUnit(path.joinToString("."))
        .setStorage(file("$generatedSourcesDir/$emojiSubgroupFilePath").toPath())

    emojiSubGroupCompilationUnit.run {
        addImport("java.util.Arrays")
        addImport("java.util.List")
    }

    val emojiSubGroupInterfaceFile = emojiSubGroupCompilationUnit.addInterface(filename).setPublic(false)

    emojiSubGroupInterfaceFile.addSingleMemberAnnotation(
        "SuppressWarnings",
        ArrayInitializerExpr(NodeList(StringLiteralExpr("unused")))
    )

    val emojisListField: FieldDeclaration = emojiSubGroupInterfaceFile.addField("List<Emoji>", "EMOJI_LIST")
    val emojisArrayCreationExpr = MethodCallExpr(NameExpr("Arrays"), "asList")

    emojiSubGroupInterfaceConstantVariables.forEach { pair ->
        pair.second.forEach {
            emojisArrayCreationExpr.addArgument(pair.first + "." + it.getVariable(0).name.toString())
        }
    }

    emojisListField.getVariable(0).setInitializer(emojisArrayCreationExpr)
    emojiSubGroupCompilationUnit.storage.get().save()
}

fun createSubGroupEmojiInterface(
    path: List<String>,
    emojiSubgroupFileName: String,
    emojiSubGroupInterfaceConstantVariables: List<FieldDeclaration>
) {
    val emojiSubgroupFilePath = "${path.joinToString("/")}/$emojiSubgroupFileName.java"
    val emojiSubGroupCompilationUnit = CompilationUnit(path.joinToString("."))
        .setStorage(file("$generatedSourcesDir/$emojiSubgroupFilePath").toPath())
    val emojiSubGroupInterfaceFile = emojiSubGroupCompilationUnit.addInterface(emojiSubgroupFileName).setPublic(false)
    emojiSubGroupCompilationUnit.run {
        addImport("java.util.Arrays")
        addImport("java.util.Collections")
    }

    emojiSubGroupInterfaceConstantVariables.forEach(emojiSubGroupInterfaceFile::addMember)
    emojiSubGroupInterfaceFile.addSingleMemberAnnotation(
        "SuppressWarnings",
        ArrayInitializerExpr(NodeList(StringLiteralExpr("unused"), StringLiteralExpr("UnnecessaryUnicodeEscape")))
    )

    emojiSubGroupCompilationUnit.storage.get().save()
}

fun getGeneratedMethodCallExprForEntries(aliases: List<StringLiteralExpr>): MethodCallExpr {
    if (aliases.isEmpty()) return MethodCallExpr("Collections.emptyList")
    if (aliases.size == 1) return MethodCallExpr("Collections.singletonList").setArguments(NodeList(aliases))
    return MethodCallExpr("Collections.unmodifiableList").addArgument(
        MethodCallExpr("Arrays.asList").setArguments(
            NodeList(aliases)
        )
    )
}

// Fix quotation marks
val replaceQuotationMarksRegex = Regex("\"")
// Fix error with aliases like: :-\
val replaceBackslashRegex = Regex("[\\\\]")

fun getAndSanitizeEmojiAliases(aliases: JsonNode): List<StringLiteralExpr> {
    return buildList {
        aliases.forEach { alias ->
            add(
                StringLiteralExpr(
                    alias.asText().replace(replaceBackslashRegex, "\\\\\\\\")
                        .replace(replaceQuotationMarksRegex, "\\\\\"")
                )
            )
        }
    }
}

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

