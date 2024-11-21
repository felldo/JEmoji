plugins {
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    //https://github.com/melix/jmh-gradle-plugin
    id("me.champeau.jmh") version "0.7.2"
    id("myproject.library-conventions")
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
                    "net.fellbaum.jemoji=$it"
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
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
}

jmh {
//    includes.addAll("some regular expression")  // include pattern (regular expression) for benchmarks to be executed
    excludes.addAll("excluded") // exclude pattern (regular expression) for benchmarks to be executed
//    iterations.set(10) // Number of measurement iterations to do.
    benchmarkMode.addAll("avgt") // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
//    batchSize.set(1) // Batch size: number of benchmark method calls per operation. (some benchmark modes can ignore this setting)
    fork.set(2) // How many times to forks a single benchmark. Use 0 to disable forking altogether
//    failOnError.set(false) // Should JMH fail immediately if any benchmark had experienced the unrecoverable error?
//    forceGC.set(false) // Should JMH force GC between iterations?
//    jvm.set("myjvm") // Custom JVM to use when forking.
//    jvmArgs.addAll("Custom JVM args to use when forking.")
//    jvmArgsAppend.addAll("Custom JVM args to use when forking (append these)")
//    jvmArgsPrepend.addAll("Custom JVM args to use when forking (prepend these)")
//    humanOutputFile.set(project.file("${project.buildDir}/reports/jmh/human.txt")) // human-readable output file
//    resultsFile.set(project.file("${project.buildDir}/reports/jmh/results.txt")) // results file
//    operationsPerInvocation.set(10) // Operations per invocation.
//    //benchmarkParameters.put("", ListProperty) // Benchmark parameters.
//    profilers.addAll("") // Use profilers to collect additional data. Supported profilers: [cl, comp, gc, stack, perf, perfnorm, perfasm, xperf, xperfasm, hs_cl, hs_comp, hs_gc, hs_rt, hs_thr, async]
//    timeOnIteration.set("1s") // Time to spend at each measurement iteration.
    resultFormat.set("JSON") // Result format type (one of CSV, JSON, NONE, SCSV, TEXT)
//    synchronizeIterations.set(false) // Synchronize iterations?
//    threads.set(4) // Number of worker threads to run with.
//    threadGroups.addAll(2,3,4) //Override thread group distribution for asymmetric benchmarks.
//    jmhTimeout.set("1s") // Timeout for benchmark iteration.
//    timeUnit.set("ms") // Output time unit. Available time units are: [m, s, ms, us, ns].
//    verbosity.set("NORMAL") // Verbosity mode. Available modes are: [SILENT, NORMAL, EXTRA]
//    warmup.set("1s") // Time to spend at each warmup iteration.
//    warmupBatchSize.set(10) // Warmup batch size: number of benchmark method calls per operation.
//    warmupForks.set(0) // How many warmup forks to make for a single benchmark. 0 to disable warmup forks.
//    warmupIterations.set(1) // Number of warmup iterations to do.
//    warmupMode.set("INDI") // Warmup mode for warming up selected benchmarks. Warmup modes are: [INDI, BULK, BULK_INDI].
//    warmupBenchmarks.addAll(".*Warmup") // Warmup benchmarks to include in the run in addition to already selected. JMH will not measure these benchmarks, but only use them for the warmup.
//
//    zip64.set(true) // Use ZIP64 format for bigger archives
//    jmhVersion.set("1.36") // Specifies JMH version
//    includeTests.set(true) // Allows to include test sources into generate JMH jar, i.e. use it when benchmarks depend on the test classes.
//    duplicateClassesStrategy.set(DuplicatesStrategy.FAIL) // Strategy to apply when encountring duplicate classes during creation of the fat jar (i.e. while executing jmhJar task)
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
    group = "jemoji"
    doLast {
        copy {
            from("${layout.buildDirectory}/libs/jemoji.jar")
            into(project.rootDir.path + "\\libs")
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
        create<MavenPublication>("JEMOJI") {
            artifactId = "jemoji"
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
                name.set("Java Emoji")
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
}

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

val generatedSourcesDir = "${layout.buildDirectory.get()}/generated/jemoji"

sourceSets {
    main {
        java.setSrcDirs(java.srcDirs + files(generatedSourcesDir))
    }
}
