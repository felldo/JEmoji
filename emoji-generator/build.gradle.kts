plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass = "net.fellbaum.jemoji.generator.MainKt"
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.htmlunit)
    implementation(libs.javapoet)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.jcabi.github)
    implementation(files(rootDir.resolve("libs/jemoji.jar")))
}

kotlin {
    jvmToolchain(25)
}

val mainClassName = "net.fellbaum.jemoji.generator.MainKt"

tasks.register<JavaExec>("generate") {
    group = "jemoji"
    description = "Generate emoji data (without CLDR translations)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = mainClassName
    args("--rootDir=${rootProject.rootDir.absolutePath}")
}

tasks.register<JavaExec>("generateAll") {
    group = "jemoji"
    description = "Generate emoji data including all CLDR translations"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = mainClassName
    args("--all", "--rootDir=${rootProject.rootDir.absolutePath}")
}
