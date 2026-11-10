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
