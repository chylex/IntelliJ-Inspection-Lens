rootProject.name = "InspectionLens"

pluginManagement {
	plugins {
		kotlin("jvm") version "1.9.24" // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#bundled-stdlib-versions
		id("org.jetbrains.intellij.platform") version "2.6.0" // https://github.com/JetBrains/intellij-platform-gradle-plugin/releases	
	}
}
