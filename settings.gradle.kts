rootProject.name = "InspectionLens"

pluginManagement {
	plugins {
		kotlin("jvm") version "2.3.0" // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#bundled-stdlib-versions
		id("org.jetbrains.intellij.platform") version "2.10.5" // https://github.com/JetBrains/intellij-platform-gradle-plugin/releases
	}
}
