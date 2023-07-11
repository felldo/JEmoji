import com.fasterxml.jackson.annotation.JsonRawValue
//import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
/*import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.EnumConstantDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.type.TypeParameter
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.printer.configuration.DefaultConfigurationOption
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration*/
import net.fellbaum.jemoji.Fitzpatrick
import net.fellbaum.jemoji.HairStyle
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.toHexString
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.util.stream.Collectors

plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.github.ben-manes.versions") version "0.47.0"

    //https://github.com/melix/jmh-gradle-plugin
    id("me.champeau.jmh") version "0.7.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
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
//    excludes.addAll("some regular expression") // exclude pattern (regular expression) for benchmarks to be executed
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
//    resultFormat.set("CSV") // Result format type (one of CSV, JSON, NONE, SCSV, TEXT)
//    synchronizeIterations.set(false) // Synchronize iterations?
//    threads.set(4) // Number of worker threads to run with.
//    threadGroups.addAll(2,3,4) //Override thread group distribution for asymmetric benchmarks.
//    jmhTimeout.set("1s") // Timeout for benchmark iteration.
    timeUnit.set("ms") // Output time unit. Available time units are: [m, s, ms, us, ns].
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

tasks.named("build") {
    finalizedBy("copyJarToProject")
}

tasks.register("copyJarToProject") {
    doLast {
        copy {
            from("$buildDir/libs/jemoji.jar")
            into(project.rootDir.path + "\\libs")
        }
    }
}

publishing {
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
                url.set("https://github.com/felldo/jemoji")

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
                    connection.set("scm:git:https://github.com/felldo/jemoji.git")
                    developerConnection.set("scm:git:git@github.com:felldo/jemoji.git")
                    url.set("https://github.com/felldo/jemoji")
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
                username = if (hasProperty("NEXUS_USERNAME")) project.property("NEXUS_USERNAME") as String else null
                password = if (hasProperty("NEXUS_PASSWORD")) project.property("NEXUS_PASSWORD") as String else null
            }
        }
    }
}

signing {
    val signingKey = findProperty("JEMOJI_SINGING_SECRET_KEY_RING_FILE") as String
    val signingKeyId = findProperty("JEMOJI_SIGNING_KEY_ID") as String
    val signingPassword = findProperty("JEMOJI_SIGNING_PASSWORD") as String

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["JEMOJI"])
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
        classpath("com.fasterxml.jackson.core:jackson-databind:2.15.2")
        classpath("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
        classpath("com.squareup.okhttp3:okhttp:4.9.3")

        classpath("org.jsoup:jsoup:1.16.1")
        classpath("com.github.javaparser:javaparser-symbol-solver-core:3.25.4")
        classpath(files(project.rootDir.path + "\\libs\\jemoji.jar"))
    }
}

tasks.register("generateEmojis") {
    //dependsOn("build")
    doLast {

        val unicodeTestDataUrl = "https://unicode.org/Public/emoji/latest/emoji-test.txt"

        val client = OkHttpClient()
        val mapper = jacksonObjectMapper()

        val githubEmojiAliasMap = getGithubEmojiAliasMap(client, mapper)
        val emojiTerraMap = getEmojiTerraMap()
        val discordAliases = getDiscordAliasMap(client, mapper)

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
                subGroupLines.drop(1)
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

                        val completeDiscordAliases = buildSet {

                            discordAliases[codepointsString]?.let { addAll(it) }
                            emojiTerraInfo?.discordCode?.let { add(it) }
                        }

                        val completeGitHubAliases = buildSet {
                            githubEmojiAliasMap[cpOrigString]?.let { addAll(it.map { it.first }.toList()) }
                            emojiTerraInfo?.githubCode?.let { add(it) }
                        }

                        val completeSlackAliases = buildSet {
                            emojiTerraInfo?.slackCode?.let { add(it) }
                        }

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
                            subGroupName
                        )
                    }.toList()
            }
        }

        //val fileRead = File("$projectDir/src/main/resources/emojis-override.json") TODO: Allow specific overrides or additions to i.e. aliases

        val file = File("$projectDir/src/main/resources/emojis.json")

        file.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allUnicodeEmojis))
    }
}

data class EmojiTerraInfo(
    val discordCode: String?,
    val githubCode: String?,
    val slackCode: String?,
    val keywords: List<String>
);

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
    return mapper.readTree(
        client.newCall(Request.Builder().url(gitHubEmojiAliasUrl).build()).execute().body!!.string()
    )
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
                    document.getElementById("copy-emoji")?.`val`()!!,
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
    val subgroup: String
)

fun getDiscordAliasMap(client: OkHttpClient, mapper: ObjectMapper): Map<String, List<String>> {
    val discordEmojiAliasUrl = "https://emzi0767.gl-pages.emzi0767.dev/discord-emoji/discordEmojiMap.json"

    val json = mapper.readTree(
        client.newCall(Request.Builder().url(discordEmojiAliasUrl).build()).execute().body!!.string()
    )

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

/*

// Try to create an enum with all emojis to work nicely with them and remove the dependency on Jackson

val generatedSourcesDir = "$buildDir/generated/jemoji"
sourceSets {
    main {
        java.setSrcDirs(java.srcDirs + files(generatedSourcesDir))
    }
}

tasks.register("generateJavaSourceFiles") {
    doLast {
        val emojiNode =
            jacksonObjectMapper().readTree(file(projectDir.absolutePath + "\\src\\main\\resources\\emojis.json"))

        val fileName = "Emoji"
        val path = listOf("net", "fellbaum", "jemoji")

        val emojisPath = "${path.joinToString("/")}/$fileName.java"
        val compilationUnit = CompilationUnit(path.joinToString("."))

        compilationUnit.setStorage(file("$generatedSourcesDir/$emojisPath").toPath())

        // IMPORTS
        compilationUnit.addImport("java.util.Arrays")
        compilationUnit.addImport("java.util.List")
        compilationUnit.addImport("java.util.ArrayList")
        compilationUnit.addImport("java.io.UnsupportedEncodingException")
        compilationUnit.addImport("java.net.URLEncoder")
        compilationUnit.addImport("java.util.Map")
        compilationUnit.addImport("java.util.Collections")
        compilationUnit.addImport("java.util.function.Function")
        compilationUnit.addImport("java.nio.charset.StandardCharsets")
        compilationUnit.addImport("java.util.stream.Collectors")

        val enumFile = compilationUnit.addEnum(fileName)

        //////////////////////////
        // ENTRIES
        //////////////////////////

        // ALIASES
        val nodeList = NodeList<EnumConstantDeclaration>()

        // FIX DESCRIPTION ERRORS THAT CAUSE COMPILATION ERRORS WITH ENUM NAMES
        // Basic allowed characters
        val descriptionReplaceRegex = Regex("[^A-Z0-9_]+")
        // Fix error with emoji names like Keycap: *
        val descriptionReplaceStarRegex = Regex("\\*")
        // Fix error with emoji names like Keycap: #
        val descriptionReplaceHashRegex = Regex("#")
        // Fix error with emoji names like: A button (blood type)
        //which results in an ending _ in the name
        val descriptionReplaceEndingUnderscoreRegex = Regex("_$")

        emojiNode.forEach {
            val discordAliases = getAndSanitizeEmojiAliases(it.get("discordAliases"))
            val slackAliases = getAndSanitizeEmojiAliases(it.get("slackAliases"))
            val githubAliases = getAndSanitizeEmojiAliases(it.get("githubAliases"))

            val qualification = it.get("qualification").asText()

            var constantName = it.get("description").asText()
                .uppercase()
                .replace(descriptionReplaceStarRegex, "STAR")
                .replace(descriptionReplaceHashRegex, "HASH")
                .replace(
                    descriptionReplaceRegex,
                    "_"
                )
                .replace(
                    descriptionReplaceEndingUnderscoreRegex,
                    ""
                ) + (if (qualification != "fully-qualified") "_" + qualification.uppercase()
                .replace(descriptionReplaceRegex, "_") else "")

            // Special cases for emoji names that start with 1st, 2nd, 3rd
            if (constantName.startsWith("1ST")) {
                constantName = "FIR" + constantName.substring(1)
            } else if (constantName.startsWith("2ND")) {
                constantName = "SEC" + constantName.substring(1)
            } else if (constantName.startsWith("3RD")) {
                constantName = "THI" + constantName.substring(1)
            }

            val enumConstantDeclaration = EnumConstantDeclaration(constantName)
                .addArgument(StringLiteralExpr(it.get("emoji").asText()))
                .addArgument(StringLiteralExpr(it.get("unicode").asText()))
                .addArgument(getGeneratedMethodCallExprForEntries(discordAliases))
                .addArgument(getGeneratedMethodCallExprForEntries(slackAliases))
                .addArgument(getGeneratedMethodCallExprForEntries(githubAliases))
                .addArgument(BooleanLiteralExpr(it.get("hasFitzpatrick").asBoolean()))
                .addArgument(BooleanLiteralExpr(it.get("hasHairStyle").asBoolean()))
                .addArgument(DoubleLiteralExpr(it.get("version").asDouble()))
                .addArgument(MethodCallExpr("Qualification.fromString").addArgument(StringLiteralExpr(qualification)))
                .addArgument(StringLiteralExpr(it.get("description").asText()))
            nodeList.add(enumConstantDeclaration)
        }

        nodeList.groupBy { it.name.asString() }.onEach {
            if (it.value.size > 1) {
                it.value.forEachIndexed { index, enumConstantDeclaration ->
                    enumConstantDeclaration.setName(enumConstantDeclaration.name.asString() + "_$index")
                }
            }
        }

        enumFile.setEntries(nodeList)

        //////////////////////////
        // FIELDS
        //////////////////////////

        val valuesVarDec = VariableDeclarator()
        valuesVarDec.setName("VALUES")
        valuesVarDec.setType("$fileName[]")
        valuesVarDec.setInitializer(
            """
         values()
         """.trimIndent()
        )

        enumFile.addPrivateField("$fileName[]", "VALUES")
            .setFinal(true)
            .setStatic(true)
            .setVariable(0, valuesVarDec)

        val emojiToObjectVarDec = VariableDeclarator()
        emojiToObjectVarDec.setName("EMOJI_TO_OBJECT")
        emojiToObjectVarDec.setType("Map<String, $fileName>")
        emojiToObjectVarDec.setInitializer(
            """
         Collections.unmodifiableMap(
                Arrays.stream(VALUES)
                    .collect(Collectors.toMap($fileName::getEmoji, Function.identity())))
         """.trimIndent()
        )

        enumFile.addPrivateField("Map<String, $fileName>", "EMOJI_TO_OBJECT")
            .setFinal(true)
            .setStatic(true)
            .setVariable(0, emojiToObjectVarDec)

        val allAliasesField = enumFile.addPrivateField("List<String>", "allAliases").setFinal(true)
        val emojiField = enumFile.addPrivateField(String::class.java, "emoji").setFinal(true)
        val unicodeField = enumFile.addPrivateField(String::class.java, "unicode").setFinal(true)
        val discordAliasesField =
            enumFile.addPrivateField(TypeParameter("List<String>"), "discordAliases").setFinal(true)
        val slackAliasesField = enumFile.addPrivateField(TypeParameter("List<String>"), "slackAliases").setFinal(true)
        val githubField = enumFile.addPrivateField(TypeParameter("List<String>"), "githubAliases").setFinal(true)
        val hasFitzpatrickField = enumFile.addPrivateField(Boolean::class.java, "hasFitzpatrick").setFinal(true)
        val hasHairStyleField = enumFile.addPrivateField(Boolean::class.java, "hasHairStyle").setFinal(true)
        val versionField = enumFile.addPrivateField(Double::class.java, "version").setFinal(true)
        val qualificationField = enumFile.addPrivateField("Qualification", "qualification").setFinal(true)
        val descriptionField = enumFile.addPrivateField(String::class.java, "description").setFinal(true)

        //////////////////////////
        // CONSTRUCTOR
        //////////////////////////
        val enumConstructor = enumFile.addConstructor()

        // Parameter
        enumConstructor.addParameter(String::class.java, "emoji")
        enumConstructor.addParameter(String::class.java, "unicode")
        enumConstructor.addParameter(TypeParameter("List<String>"), "discordAliases")
        enumConstructor.addParameter(TypeParameter("List<String>"), "slackAliases")
        enumConstructor.addParameter(TypeParameter("List<String>"), "githubAliases")
        enumConstructor.addParameter(Boolean::class.java, "hasFitzpatrick")
        enumConstructor.addParameter(Boolean::class.java, "hasHairStyle")
        enumConstructor.addParameter(Double::class.java, "version")
        enumConstructor.addParameter("Qualification", "qualification")
        enumConstructor.addParameter(String::class.java, "description")

        // BODY
        enumConstructor.createBody()
            .addStatement("this.emoji = emoji;")
            .addStatement("this.unicode = unicode;")
            .addStatement("this.discordAliases = discordAliases;")
            .addStatement("this.slackAliases = slackAliases;")
            .addStatement("this.githubAliases = githubAliases;")
            .addStatement("this.hasFitzpatrick = hasFitzpatrick;")
            .addStatement("this.hasHairStyle = hasHairStyle;")
            .addStatement("this.version = version;")
            .addStatement("this.qualification = qualification;")
            .addStatement("this.description = description;")
            .addStatement("List<String> aliases = new ArrayList<>();")
            .addStatement("aliases.addAll(getDiscordAliases());")
            .addStatement("aliases.addAll(getGithubAliases());")
            .addStatement("aliases.addAll(getSlackAliases());")
            .addStatement("allAliases = Collections.unmodifiableList(aliases);")

        // Generate methods below the constructor

        emojiField.createGetter().setJavadocComment(
            """
                Gets the emoji.
                
                @return The emoji
            """.trimIndent()
        )
        unicodeField.createGetter().setJavadocComment(
            """
                Gets the unicode representation of the emoji as a string i.e. \uD83D\uDC4D.
                
                @return The unicode representation of the emoji
            """.trimIndent()
        )
        discordAliasesField.createGetter().setJavadocComment(
            """
                Gets the Discord aliases for this emoji.
                
                @return The Discord aliases for this emoji.
            """.trimIndent()
        )
        slackAliasesField.createGetter().setJavadocComment(
            """
                Gets the GitHub aliases for this emoji.
                
                @return The Slack aliases for this emoji.
            """.trimIndent()
        )
        githubField.createGetter().setJavadocComment(
            """
                Gets all the aliases for this emoji.
                
                @return All the aliases for this emoji.
            """.trimIndent()
        )
        hasFitzpatrickField.createGetter().setName("hasFitzpatrickComponent").setJavadocComment(
            """
                Checks if this emoji has a fitzpatrick modifier.
                
                @return True if this emoji has a fitzpatrick modifier, false otherwise.
            """.trimIndent()
        )
        hasHairStyleField.createGetter().setName("hasHairStyleComponent").setJavadocComment(
            """
                Checks if this emoji has a hairstyle modifier.
                
                @return True if this emoji has a hairstyle modifier, false otherwise.
            """.trimIndent()
        )
        versionField.createGetter().setJavadocComment(
            """
                Gets the version this emoji was added to the unicode consortium.
                
                @return The version this emoji was added to the unicode consortium.
            """.trimIndent()
        )
        qualificationField.createGetter().setJavadocComment(
            """
                Gets the qualification of this emoji.
                
                @return The qualification of this emoji.
            """.trimIndent()
        )
        descriptionField.createGetter().setJavadocComment(
            """
                Gets the description of this emoji.
                
                @return The description of this emoji.
            """.trimIndent()
        )

        // Generate additional Methods

        enumFile.addMethod("getAllAliases", Modifier.Keyword.PUBLIC)
            .setType("List<String>")
            .setJavadocComment(
                """
                Gets all the aliases for this emoji.
                
                @return All the aliases for this emoji.
            """.trimIndent()
            )
            .createBody()
            .addStatement("return allAliases;")

        enumFile.addMethod("getHtmlDecimalCode", Modifier.Keyword.PUBLIC)
            .setType(String::class.java)
            .setJavadocComment(
                """
                Gets the HTML decimal code for this emoji.
                
                @return The HTML decimal code for this emoji.
            """.trimIndent()
            )
            .createBody()
            .addStatement("return getEmoji().codePoints().mapToObj(operand -> \"&#\" + operand).collect(Collectors.joining(\";\")) + \";\";")


        enumFile.addMethod("getHtmlHexadecimalCode", Modifier.Keyword.PUBLIC)
            .setType(String::class.java)
            .setJavadocComment(
                """
                Gets the HTML hexadecimal code for this emoji.
                
                @return The HTML hexadecimal code for this emoji.
            """.trimIndent()
            )
            .createBody()
            .addStatement("return getEmoji().codePoints().mapToObj(operand -> \"&#x\" + Integer.toHexString(operand).toUpperCase()).collect(Collectors.joining(\";\")) + \";\";")


        enumFile.addMethod("getVariations", Modifier.Keyword.PUBLIC)
            .setType("List<Emoji>")
            .setJavadocComment(
                """
                Gets variations of this emoji with different Fitzpatrick or HairStyle modifiers, if there are any.
                The returned list does not include this emoji itself.
                
                @return Variations of this emoji with different Fitzpatrick or HairStyle modifiers, if there are any.
            """.trimIndent()
            )
            .createBody()
            .addStatement("final String baseEmoji = HairStyle.removeHairStyle(Fitzpatrick.removeFitzpatrick(emoji));")
            .addStatement(
                """
                return EmojiManager.getAllEmojis()
                .parallelStream()
                .filter(emoji -> HairStyle.removeHairStyle(Fitzpatrick.removeFitzpatrick(emoji.getEmoji())).equals(baseEmoji))
                .filter(emoji -> !emoji.equals(this))
                .collect(Collectors.toList());
            """.trimIndent()
            )

        enumFile.addMethod("getURLEncoded", Modifier.Keyword.PUBLIC)
            .setType(String::class.java)
            .setJavadocComment(
                """
                Gets the URL encoded emoji.
                
                @return The URL encoded emoji
            """.trimIndent()
            )
            .createBody()
            .addStatement(
                """
                try {
                   return URLEncoder.encode(getEmoji(), StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            """.trimIndent()
            )

        enumFile.addMethod("toString", Modifier.Keyword.PUBLIC)
            .setType(String::class.java)
            .addAnnotation(Override::class.java)
            .createBody()
            .addStatement(
                """
                return "$fileName{" +
                "emoji='" + emoji + '\'' +
                ", unicode='" + unicode + '\'' +
                ", discordAliases=" + discordAliases +
                ", slackAliases=" + slackAliases +
                ", githubAliases=" + githubAliases +
                ", hasFitzpatrick=" + hasFitzpatrick +
                ", hasHairStyle=" + hasHairStyle +
                ", version=" + version +
                ", qualification=" + qualification +
                ", description='" + description + '\'' +
                '}';
            """.trimIndent()
            )

        val config = DefaultPrinterConfiguration()
            .addOption(DefaultConfigurationOption(DefaultPrinterConfiguration.ConfigOption.ORDER_IMPORTS, true))
            .addOption(DefaultConfigurationOption(DefaultPrinterConfiguration.ConfigOption.END_OF_LINE_CHARACTER, "\n"))
            .addOption(
                DefaultConfigurationOption(
                    DefaultPrinterConfiguration.ConfigOption.COLUMN_ALIGN_PARAMETERS,
                    true
                )
            )
            .addOption(
                DefaultConfigurationOption(
                    DefaultPrinterConfiguration.ConfigOption.COLUMN_ALIGN_FIRST_METHOD_CHAIN,
                    true
                )
            )

        compilationUnit.storage.get().save()
        //println(DefaultPrettyPrinter(config).print(compilationUnit))
    }
}

fun getGeneratedMethodCallExprForEntries(aliases: List<StringLiteralExpr>): MethodCallExpr {
    if (aliases.isEmpty()) return MethodCallExpr("Collections.emptyList")
    if (aliases.size == 1) return MethodCallExpr("Collections.singletonList").setArguments(NodeList(aliases))
    return MethodCallExpr("Arrays.asList").setArguments(NodeList(aliases))
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
*/