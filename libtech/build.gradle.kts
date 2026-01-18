plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.shadow)
}

group = "dev.munky"
version = "1.0.0"

dependencies {
    compileOnly(files(rootDir.resolve(".server/Server/HytaleServer.jar")))

    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        exclude("kotlin/**")
        archiveFileName.set("LibTech.jar")
        destinationDirectory.set(file(rootDir.resolve("./out/")))
    }
}