package com.chylex.intellij.inspectionlens.debug

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.RangeHighlighter

data class Highlighter(val hashCode: Int, val layer: Int, val severity: HighlightSeverity, val description: String) {
	constructor(highlighter: RangeHighlighter, info: HighlightInfo) : this(System.identityHashCode(highlighter), highlighter.layer, info.severity, info.description)
}
