plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)

    compileOnly(project(":libtech"))
    compileOnly(project(":kotlin-plugin"))
    compileOnly(files(rootDir.resolve(".server/Server/HytaleServer.jar")))
}

tasks {
    shadowJar {
        exclude("kotlin/**")
        archiveFileName.set("FactoryMod.jar")
        destinationDirectory.set(file(rootDir.resolve("./out/")))
    }

    register<Copy>("moveModsToServers") {
        outputs.upToDateWhen { false }
        doNotTrackState("Instrumentation needs to re-run every time")

        val jarDir = rootDir.resolve("./out/")
        val server1Mods = "Server/mods/"
        val server2Mods = "Server2/mods/"

        from(jarDir) {
            into(server1Mods)
        }

        from(jarDir) {
            into(server2Mods)
        }

        into(rootDir.resolve(".server/"))
    }
}