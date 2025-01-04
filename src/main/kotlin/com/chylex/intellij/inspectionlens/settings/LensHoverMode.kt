package com.chylex.intellij.inspectionlens.settings

enum class LensHoverMode(val description: String) {
	DISABLED("Disabled"),
	DEFAULT("Left click shows intentions, middle click jumps to highlight"),
	SWAPPED("Left click jumps to highlight, middle click shows intentions")
}
