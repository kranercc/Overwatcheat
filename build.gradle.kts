import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow

plugins {
    id("overwatcheat-kotlin-project")

    application

    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.jire.overwatcheat"
version = "5.1.0"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

dependencies {
    implementation(libs.fastutil)
    implementation(libs.javacv.platform)
    implementation(libs.vis.ui)

    implementation(libs.affinity)
    implementation(libs.chronicle.core)

    implementation(libs.jna)
    implementation(libs.jna.platform)

    implementation(libs.gdx)
    implementation(libs.gdx.platform)

    implementation(libs.gdx.box2d)
    implementation(libs.gdx.box2d.platform)

    implementation(libs.gdx.freetype)
    implementation(libs.gdx.freetype.platform)

    implementation(libs.gdx.backend.lwjgl3)

    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)
}

application {
    applicationName = "Overwatcheat"
    mainClass.set("org.jire.overwatcheat.Main")
    applicationDefaultJvmArgs += arrayOf(
        "-Xmx8g",
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.time=ALL-UNNAMED",
        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED"
    )
}

tasks {
    configureShadowJar()
    configureOverwatcheat()
}

fun TaskContainerScope.configureShadowJar() {
    shadowJar {
        archiveBaseName.set("Overwatcheat")
        archiveClassifier.set("")
        archiveVersion.set("${project.version}")

        isZip64 = true
        //minimize() // needs to be updated for Java 19 support
    }
    named<Zip>("distZip").configure {
        enabled
    }
    named<Tar>("distTar").configure {
        enabled = false
    }
}

fun TaskContainerScope.configureOverwatcheat() {
    register("overwatcheat") {
        dependsOn(shadowJar)
        doLast {
            val version = version
            val name = "Overwatcheat $version"

            val buildDir = file("build/")

            val dir = buildDir.resolve(name)
            if (dir.exists()) dir.deleteRecursivelyOrThrow()
            dir.mkdirs()

            val jarName = "${name}.jar"
            val jar = dir.resolve(jarName)
            val allJar = buildDir.resolve("libs/Overwatcheat-${version}.jar")
            allJar.copyTo(jar, true)

            dir.writeStartBat(name, jarName)

            fun File.copyFromRoot(path: String) = file(path).copyTo(resolve(path), true)

            dir.copyFromRoot("overwatcheat.cfg")
            dir.copyFromRoot("LICENSE.txt")
            dir.copyFromRoot("README.md")
        }
    }
}

fun File.writeStartBat(name: String, jarName: String) =
    resolve("Start ${name}.bat")
        .writeText(
            """@echo off
cd /d "%~dp0"
title $name
java ${application.applicationDefaultJvmArgs.joinToString(" ")} -jar "$jarName"
pause"""
        )
