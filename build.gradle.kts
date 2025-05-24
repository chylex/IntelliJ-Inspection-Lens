@file:Suppress("ConvertLambdaToReference")

plugins {
	kotlin("jvm")
	id("org.jetbrains.intellij.platform")
}

group = "com.chylex.intellij.inspectionlens"
version = "1.5.2.902"

repositories {
	mavenCentral()
	
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		rustRover("2025.1.2", useInstaller = false)
		bundledPlugin("tanvd.grazi")
	}
	
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild.set("233.11361.10")
			untilBuild.set(provider { null })
		}
	}
}

kotlin {
	jvmToolchain(17)
	
	compilerOptions {
		freeCompilerArgs = listOf(
			"-X" + "jvm-default=all",
			"-X" + "lambdas=indy"
		)
	}
}

tasks.test {
	useJUnitPlatform()
}
