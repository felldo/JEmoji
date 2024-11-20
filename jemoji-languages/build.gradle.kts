plugins {
    `java-library`
    `maven-publish`
    id("org.jreleaser") version "1.15.0"

    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":jemoji"))
    testImplementation(project(":jemoji"))
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit4 test framework
            useJUnitJupiter("5.11.3")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    systemProperty("file.encoding", "UTF-8")
}/*
val stagingDir: Provider<Directory> = layout.buildDirectory.dir("staging-deploy")
publishing {
    if (project.gradle.startParameter.taskNames.contains("publish")
        or project.gradle.startParameter.taskNames.contains("publishToMavenLocal")
    ) {
        project.version = project.version.toString().replace("-SNAPSHOT", "")
    }
    publications {
        create<MavenPublication>("JEMOJI_LANGUAGES") {
            artifactId = "jemoji-languages"
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
                name.set("Java Emoji Languages")
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
            url = stagingDir.get().asFile.toURI()
        }
    }
}

jreleaser {
    gitRootSearch = true
    signing {
        active = org.jreleaser.model.Active.ALWAYS
        armored = true

    }

    release {
        github {
            repoOwner = "felldo"
            repoUrl = "https://github.com/felldo/JEmoji"
            skipRelease = true
            skipTag = true
            sign = true
            branch = "main"
            branchPush = "main"
            overwrite = true
        }
    }

    deploy {
        maven {
            //Portal Publisher API
            mavenCentral {
                create("sonatype") {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    applyMavenCentralRules = true
                    //snapshotSupported = true
                    println(stagingDir.get().toString())
                    stagingRepository(stagingDir.get().toString())
                }
            }

        }
    }
}*/
fun findPropertyOrNull(name: String) = if (hasProperty(name)) project.property(name) as String else null

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
        docTitle = "JEmoji Languages ${project.version}"
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