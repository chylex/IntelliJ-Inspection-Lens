package com.chylex.intellij.inspectionlens.settings

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.util.xmlb.annotations.Tag

@Tag("severity")
data class StoredSeverity(var name: String = "", var priority: Int = 0) {
	constructor(severity: HighlightSeverity) : this(severity.displayCapitalizedName, severity.myVal)
}
