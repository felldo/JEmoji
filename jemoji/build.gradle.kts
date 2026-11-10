plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.jmh)
    id("myproject.library-conventions")
}

library {
    title = "JEmoji"
}

dependencies {
    implementation(libs.jackson.databind)
}

dependencies {
    api(libs.jspecify)
}

jmh {
    // All options: https://github.com/melix/jmh-gradle-plugin#configuration-options
    excludes.addAll("excluded")
    benchmarkMode.addAll("avgt")
    fork.set(2)
    resultFormat.set("JSON")
    jmhVersion.set("1.37")
}

//Generate sources before compiling if they do not exist
tasks.named("compileJava") {
    if (!file("./build/generated/jemoji/net/fellbaum/jemoji/Emojis.java").exists()) {
        dependsOn(":generate")
    }
}

tasks.named("build") {
    finalizedBy("copyJarToProject")
}

tasks.register("copyJarToProject") {
    description = "Copy the generated jar file to the root project's libs directory for easier access"
    group = "jemoji"
    doLast {
        val libsDir = layout.buildDirectory.dir("libs").get().asFile
        val targetDir = project.rootDir.resolve("libs")
        val jarFile = libsDir.listFiles()?.find { it.name.matches(Regex("jemoji-\\d+\\.\\d+\\.\\d+\\.jar")) }
        if (jarFile != null) {
            val targetFile = targetDir.resolve("jemoji.jar")
            jarFile.copyTo(targetFile, overwrite = true)
        } else {
            println("No valid jemoji-<VERSION>.jar found!")
        }
    }
}

val generatedSourcesDir = "${layout.buildDirectory.get()}/generated/jemoji"

sourceSets {
    main {
        java.setSrcDirs(java.srcDirs + files(generatedSourcesDir))
    }
}
