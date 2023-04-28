import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    id("com.github.ben-manes.versions") version "0.46.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
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

// Apply a specific Java toolchain to ease working on different environments.
java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
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
                username = findProperty("NEXUS_USERNAME") as String
                password = findProperty("NEXUS_PASSWORD") as String
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

tasks.javadoc {
    isFailOnError = false
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.fasterxml.jackson.core:jackson-databind:2.15.0")
        classpath("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
        classpath("com.squareup.okhttp3:okhttp:4.9.3")

        classpath("org.jsoup:jsoup:1.15.4")
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

        val allUnicodeEmojis = unicodeLines.lines()
            .asSequence()
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
                    emojiDescription
                )
            }
            .toList()

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
            it.first to it.second.replace(
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
    val gitHubAliases: Set<String>,
    val slackAliases: Set<String>,
    var hasFitzpatrick: Boolean,
    var hasHairStyle: Boolean,
    val version: Double,
    val qualification: String,
    val description: String
)

fun getDiscordAliasMap(client: OkHttpClient, mapper: ObjectMapper): Map<String, List<String>> {
    val discordEmojiAliasUrl = "https://emzi0767.gl-pages.emzi0767.dev/discord-emoji/discordEmojiMap.json"

    val json = mapper.readTree(
        client.newCall(Request.Builder().url(discordEmojiAliasUrl).build()).execute().body!!.string()
    )

    return buildMap {
        for (jsonNode in json.get("emojiDefinitions")) {
            val aliases = buildList {
                jsonNode.get("names").forEach { add(it.asText()) }
            }

            val key = jsonNode.get("surrogates").asText()

            put(key, aliases)
        }
    }
}
