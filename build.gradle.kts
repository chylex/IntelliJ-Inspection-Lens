@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.8.0"
	id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.chylex.intellij.inspectionlens"
version = "1.3.0"

repositories {
	mavenCentral()
}

intellij {
	version.set("233.11555.11-EAP-SNAPSHOT")
	updateSinceUntilBuild.set(false)
	
	plugins.add("tanvd.grazi")
}

kotlin {
	jvmToolchain(17)
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.patchPluginXml {
	sinceBuild.set("233.11361.10")
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
