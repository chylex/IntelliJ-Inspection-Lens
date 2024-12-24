package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.LINES_IN_RANGE
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes

@JvmInline
internal value class EditorLensLineBackground(private val highlighter: RangeHighlighter) {
	val isInvalid
		get() = !highlighter.isValid
	
	fun onFoldRegionsChanged(editor: Editor, severity: LensSeverity) {
		if (highlighter is RangeHighlighterEx) {
			highlighter.textAttributes = getAttributes(editor, highlighter.startOffset, highlighter.endOffset, severity)
		}
	}
	
	fun hide(editor: Editor) {
		editor.markupModel.removeHighlighter(highlighter)
	}
	
	companion object {
		fun show(editor: Editor, info: HighlightInfo): EditorLensLineBackground {
			val startOffset = info.actualStartOffset
			val endOffset = (info.actualEndOffset - 1).coerceAtLeast(startOffset)
			
			val severity = LensSeverity.from(info.severity)
			val layer = getHighlightLayer(severity)
			val attributes = getAttributes(editor, startOffset, endOffset, severity)
			
			return EditorLensLineBackground(editor.markupModel.addRangeHighlighter(startOffset, endOffset, layer, attributes, LINES_IN_RANGE))
		}
		
		private fun getHighlightLayer(severity: LensSeverity): Int {
			return HighlighterLayer.CARET_ROW - 100 - severity.ordinal
		}
		
		private fun getAttributes(editor: Editor, startOffset: Int, endOffset: Int, severity: LensSeverity): TextAttributes? {
			return if (editor.foldingModel.let { it.isOffsetCollapsed(startOffset) || it.isOffsetCollapsed(endOffset) })
				null
			else
				severity.lineAttributes
		}
	}
}
