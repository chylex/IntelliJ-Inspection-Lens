package com.chylex.intellij.inspectionlens.editor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.LINES_IN_RANGE
import com.intellij.openapi.editor.markup.RangeHighlighter

@JvmInline
internal value class EditorLensLineBackground(private val highlighter: RangeHighlighter) {
	@Suppress("RedundantIf")
	fun shouldRecreate(info: HighlightInfo): Boolean {
		if (!highlighter.isValid) {
			return true
		}
		
		val severity = LensSeverity.from(info.severity)
		
		val currentTextAttributes = highlighter.getTextAttributes(null)
		val newTextAttributes = severity.lineAttributes
		if (currentTextAttributes !== newTextAttributes) {
			return true
		}
		
		val currentLayer = highlighter.layer
		val newLayer = getHighlightLayer(severity)
		if (currentLayer != newLayer) {
			return true
		}
		
		return false
	}
	
	fun hide(editor: Editor) {
		editor.markupModel.removeHighlighter(highlighter)
	}
	
	companion object {
		fun show(editor: Editor, info: HighlightInfo): EditorLensLineBackground {
			val startOffset = info.actualStartOffset
			val endOffset = info.actualEndOffset
			
			val severity = LensSeverity.from(info.severity)
			val layer = getHighlightLayer(severity)
			
			return EditorLensLineBackground(editor.markupModel.addRangeHighlighter(startOffset, endOffset, layer, severity.lineAttributes, LINES_IN_RANGE))
		}
		
		private fun getHighlightLayer(severity: LensSeverity): Int {
			return HighlighterLayer.CARET_ROW - 100 - severity.ordinal
		}
	}
}
