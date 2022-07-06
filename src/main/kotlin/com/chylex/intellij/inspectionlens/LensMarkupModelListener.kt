package com.chylex.intellij.inspectionlens

import com.intellij.codeInsight.daemon.impl.AsyncDescriptionSupplier
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.event.MarkupModelListener

/**
 * Listens for inspection highlights and reports them to [EditorInlayLensManager].
 */
class LensMarkupModelListener(editor: Editor) : MarkupModelListener {
	private val lens = EditorInlayLensManager.getOrCreate(editor)
	
	override fun afterAdded(highlighter: RangeHighlighterEx) {
		showIfValid(highlighter)
	}
	
	override fun attributesChanged(highlighter: RangeHighlighterEx, renderersChanged: Boolean, fontStyleOrColorChanged: Boolean) {
		showIfValid(highlighter)
	}
	
	override fun beforeRemoved(highlighter: RangeHighlighterEx) {
		lens.hide(highlighter)
	}
	
	private fun showIfValid(highlighter: RangeHighlighterEx) {
		if (!highlighter.isValid) {
			return
		}
		
		val info = HighlightInfo.fromRangeHighlighter(highlighter)
		if (info == null || info.severity.myVal <= HighlightSeverity.INFORMATION.myVal) {
			return
		}
		
		if (info is AsyncDescriptionSupplier) {
			info.requestDescription().onSuccess {
				if (highlighter.isValid) {
					showIfNonNullDescription(highlighter, info)
				}
			}
		}
		else {
			showIfNonNullDescription(highlighter, info)
		}
	}
	
	private fun showIfNonNullDescription(highlighter: RangeHighlighterEx, info: HighlightInfo) {
		if (info.description != null) {
			lens.show(highlighter, info)
		}
	}
}
