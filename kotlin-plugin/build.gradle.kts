plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(files(rootDir.resolve(".server/Server/HytaleServer.jar")))
}

tasks {
    shadowJar {
        archiveFileName.set("KotlinPlugin.jar")
        destinationDirectory.set(file(rootDir.resolve("./out/")))
    }
}