@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
	kotlin("jvm")
	id("org.jetbrains.intellij.platform")
}

group = "com.chylex.intellij.inspectionlens"
version = "1.7.0"

idea {
	module {
		excludeDirs.add(file(".kotlin"))
		excludeDirs.add(file("build"))
	}
}

repositories {
	mavenCentral()
	
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		intellijIdeaUltimate("2025.3")
		bundledPlugin("tanvd.grazi")
		
		testFramework(TestFrameworkType.JUnit5)
	}
	
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testRuntimeOnly("junit:junit:4.13.2") // https://youtrack.jetbrains.com/projects/IJPL/issues/IJPL-159134
}

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild.set("253")
			untilBuild.set(provider { null })
		}
	}
	
	pluginVerification {
		freeArgs.add("-mute")
		freeArgs.add("TemplateWordInPluginId")
		
		ides {
			recommended()
		}
	}
}

kotlin {
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
