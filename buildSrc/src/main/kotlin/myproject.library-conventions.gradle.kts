plugins {
    `java-library`
    `maven-publish`
    id("myproject.java-conventions")
    id("org.jreleaser")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

interface LibraryPluginExtension {
    val title: Property<String>
}

val extension = project.extensions.create<LibraryPluginExtension>("library").apply {
    title.convention("")
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("6.1.3")
        }
    }
}

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        locale = "en"
        charSet = "UTF-8"
        encoding = "UTF-8"
        docTitle = "${extension.title.get()} ${project.version}"
        windowTitle = "$docTitle Documentation"
        links("https://docs.oracle.com/en/java/javase/17/docs/api/")
        isUse = true
        isVersion = true
        isAuthor = true
        isSplitIndex = true

        addBooleanOption("html5", true)
        addStringOption("-release", java.targetCompatibility.majorVersion)
        addBooleanOption("Xdoclint:all,-missing", true)
    }
}


val stagingDir: Provider<Directory> = layout.buildDirectory.dir("staging-deploy")
// Matches publish, publishAll, :jemoji:publish, publishToMavenLocal, jreleaser*, ...
val isReleaseBuild = gradle.startParameter.taskNames.any { taskPath ->
    val taskName = taskPath.substringAfterLast(':')
    taskName.startsWith("publish") || taskName.startsWith("jreleaser")
}
if (isReleaseBuild) {
    project.version = project.version.toString().removeSuffix("-SNAPSHOT")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.base.archivesName.get()
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
                name.set(project.name)
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
                        timezone.set("Europe/Zurich")
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
//https://jreleaser.org/guide/latest/examples/maven/maven-central.html#_gradle
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
            branch = "master"
            branchPush = "master"
            overwrite = true
        }
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    applyMavenCentralRules = true
                    maxRetries = 100
                    //snapshotSupported = true
                    stagingRepository(stagingDir.get().toString())
                }
            }

        }
    }
}

val signingSecretKeyRingFile = providers.gradleProperty("JEMOJI_SIGNING_SECRET_KEY_RING_FILE")
val prePublishTask = tasks.register("prePublishTask") {
    description = "Check if the secrets for signing are available before publishing"
    doFirst {
        if (signingSecretKeyRingFile.orNull.isNullOrBlank()) {
            throw GradleException("Can not publish a new release because the secrets for signing are missing")
        }
    }
}

// When publishing, check if the secrets are available
tasks.named("publish") {
    dependsOn(prePublishTask)
}
