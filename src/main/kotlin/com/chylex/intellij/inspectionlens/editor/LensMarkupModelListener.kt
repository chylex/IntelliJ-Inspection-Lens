package com.chylex.intellij.inspectionlens.editor

import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.editor.markup.RangeHighlighter

/**
 * Listens for inspection highlights and reports them to [EditorLensManager].
 */
internal class LensMarkupModelListener(private val lensManagerDispatcher: EditorLensManagerDispatcher) : MarkupModelListener {
	private val settings = service<LensSettingsState>()
	
	override fun afterAdded(highlighter: RangeHighlighterEx) {
		showIfValid(highlighter)
	}
	
	override fun attributesChanged(highlighter: RangeHighlighterEx, renderersChanged: Boolean, fontStyleOrColorChanged: Boolean) {
		showIfValid(highlighter)
	}
	
	override fun beforeRemoved(highlighter: RangeHighlighterEx) {
		if (getFilteredHighlightInfo(highlighter) != null) {
			lensManagerDispatcher.hide(highlighter)
		}
	}
	
	private fun showIfValid(highlighter: RangeHighlighter) {
		runWithHighlighterIfValid(highlighter, lensManagerDispatcher::show, ::showAsynchronously)
	}
	
	private fun showAsynchronously(highlighterWithInfo: HighlighterWithInfo.Async) {
		highlighterWithInfo.requestDescription {
			if (highlighterWithInfo.highlighter.isValid && highlighterWithInfo.hasDescription) {
				lensManagerDispatcher.show(highlighterWithInfo)
			}
		}
	}
	
	fun showAllValid(highlighters: Array<RangeHighlighter>) {
		highlighters.forEach(::showIfValid)
	}
	
	fun hideAll() {
		lensManagerDispatcher.hideAll()
	}
	
	private fun getFilteredHighlightInfo(highlighter: RangeHighlighter): HighlightInfo? {
		return HighlightInfo.fromRangeHighlighter(highlighter)?.takeIf { settings.severityFilter.test(it.severity) }
	}
	
	private inline fun runWithHighlighterIfValid(highlighter: RangeHighlighter, actionForImmediate: (HighlighterWithInfo) -> Unit, actionForAsync: (HighlighterWithInfo.Async) -> Unit) {
		val info = highlighter.takeIf { it.isValid }?.let(::getFilteredHighlightInfo)
		if (info != null && !UpdateHighlightersUtil.isFileLevelOrGutterAnnotation(info)) {
			processHighlighterWithInfo(HighlighterWithInfo.from(highlighter, info), actionForImmediate, actionForAsync)
		}
	}
	
	private inline fun processHighlighterWithInfo(highlighterWithInfo: HighlighterWithInfo, actionForImmediate: (HighlighterWithInfo) -> Unit, actionForAsync: (HighlighterWithInfo.Async) -> Unit) {
		if (highlighterWithInfo is HighlighterWithInfo.Async) {
			actionForAsync(highlighterWithInfo)
		}
		else if (highlighterWithInfo.hasDescription) {
			actionForImmediate(highlighterWithInfo)
		}
	}
}
