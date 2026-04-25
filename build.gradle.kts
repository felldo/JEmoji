plugins {
    id("base")
    alias(libs.plugins.versions)
    alias(libs.plugins.gradle.versions.filter)
}

// dependencyUpdates fails in parallel mode with Gradle 9+ (https://github.com/ben-manes/gradle-versions-plugin/issues/968)
tasks.named("dependencyUpdates") {
    doFirst {
        gradle.startParameter.isParallelProjectExecutionEnabled = false
    }
}

tasks.register("generate") {
    group = "jemoji"
    description = "Generate emoji data (without CLDR translations)"
    dependsOn(":emoji-generator:generate")
}

tasks.register("generateAll") {
    group = "jemoji"
    description = "Generate emoji data including all CLDR translations"
    dependsOn(":emoji-generator:generateAll")
}

tasks.register("publishAll") {
    description = "Publish all subprojects"
    group = "publishing"
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "publish" })
    }
}
