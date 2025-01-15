@file:Suppress("ConvertLambdaToReference")

plugins {
	kotlin("jvm")
	id("org.jetbrains.intellij.platform")
}

group = "com.chylex.intellij.inspectionlens"
version = "1.5.1"

repositories {
	mavenCentral()
	
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		intellijIdeaUltimate("2023.3.3")
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
