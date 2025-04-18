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

// Create extension object
interface LibraryPluginExtension {
    val title: Property<String>
}

// Add the 'greeting' extension object to project
val extension = project.extensions.create<LibraryPluginExtension>("library").apply {
    title.convention("")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
        vendor = JvmVendorSpec.ADOPTIUM
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
}

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        locale = "en"
        charSet = "UTF-8"
        encoding = "UTF-8"
        docTitle = "${extension.title.get()} ${project.version}"
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


val stagingDir: Provider<Directory> = layout.buildDirectory.dir("staging-deploy")
publishing {
    if (project.gradle.startParameter.taskNames.contains("publish")
        or project.gradle.startParameter.taskNames.contains("publishToMavenLocal")
    ) {
        project.version = project.version.toString().replace("-SNAPSHOT", "")
    }
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
            branch = "main"
            branchPush = "main"
            overwrite = true
        }
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
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