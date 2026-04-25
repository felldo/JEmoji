plugins {
    alias(libs.plugins.shadow)
    id("myproject.library-conventions")
}

library {
    title = "JEmoji Languages"
}

dependencies {
    compileOnly(project(":jemoji"))
    testImplementation(project(":jemoji"))
}

fun findPropertyOrNull(name: String) = if (hasProperty(name)) findProperty(name) as String else null

val prePublishTask by tasks.register("prePublishTask") {
    description = "Check if secrets are available for singing before publishing"
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
