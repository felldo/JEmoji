plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.shadow)
    alias(libs.plugins.jmh)
    id("myproject.library-conventions")
}


library {
    title = "JEmoji"
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
    compileOnlyApi(libs.jspecify)
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
