package net.fellbaum.jemoji.generator

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.io.File

fun requestCLDREmojiDescriptionTranslation(
    url: String,
    client: OkHttpClient,
    mapper: ObjectMapper,
    descriptionNodeOutput: ObjectNode,
    keywordsNodeOutput: ObjectNode,
    fileName: String,
    rootDir: String
) {
    val fileHttpBuilder = url.toHttpUrl().newBuilder()
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
            descriptionNodeOutput.put(it.key, it.value.get("tts").joinToString(" ") { jsonNode -> jsonNode.asString() })
            val keywordsArray = keywordsNodeOutput.putArray(it.key)
            if (it.value.has("default")) {
                it.value.get("default").forEach { jsonNode -> keywordsArray.add(jsonNode) }
            }
        }
    }
}

fun writeContentToPublicFiles(fileName: String, content: Any, rootDir: String) {
    val publicFile = File("$rootDir/public/$fileName.json")
    val publicFileMin = File("$rootDir/public/$fileName.min.json")
    val mapper = jacksonMapperBuilder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build()
    publicFile.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(content))
    publicFileMin.writeText(mapper.writeValueAsString(content))
}
