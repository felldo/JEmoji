plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.shadow)
    id("myproject.library-conventions")
}

library {
    title = "JEmoji Languages"
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
                    "net.fellbaum.jemoji.languages=$it"
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

dependencies {
    compileOnly(project(":jemoji"))
    testImplementation(project(":jemoji"))
}

fun findPropertyOrNull(name: String) = if (hasProperty(name)) findProperty(name) as String else null

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
