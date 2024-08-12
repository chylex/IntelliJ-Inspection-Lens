@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.8.0"
	id("org.jetbrains.intellij") version "1.17.0"
}

group = "com.chylex.intellij.inspectionlens"
version = "1.4.1"

repositories {
	mavenCentral()
}

intellij {
	version.set("2024.2")
	updateSinceUntilBuild.set(false)
	
	plugins.add("tanvd.grazi")
	plugins.add("com.intellij.classic.ui:242.20224.159")
}

kotlin {
	jvmToolchain(17)
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.patchPluginXml {
	sinceBuild.set("242")
}

tasks.buildSearchableOptions {
	enabled = false
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions.freeCompilerArgs = listOf(
		"-Xjvm-default=all"
	)
}
