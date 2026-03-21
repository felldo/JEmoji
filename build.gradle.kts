plugins {
    id("base")
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
    group = "publishing"
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "publish" })
    }
}
