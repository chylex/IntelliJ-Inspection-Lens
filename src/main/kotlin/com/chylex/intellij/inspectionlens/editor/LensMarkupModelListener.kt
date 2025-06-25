package com.chylex.intellij.inspectionlens.editor

import com.chylex.intellij.inspectionlens.InspectionLens
import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil.isFileLevelOrGutterAnnotation
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
	
	fun showAllValid(highlighters: Array<RangeHighlighter>) {
		if (InspectionLens.SHOW_LENSES) {
			highlighters.forEach(::showIfValid)
		}
	}
	
	private fun showIfValid(highlighter: RangeHighlighter) {
		if (!InspectionLens.SHOW_LENSES) {
			return
		}
		
		val info = highlighter.takeIf { it.isValid }?.let(::getFilteredHighlightInfo)
		if (info == null || isFileLevelOrGutterAnnotation(info)) {
			return
		}
		
		val highlighterWithInfo = HighlighterWithInfo.from(highlighter, info)
		processHighlighterWithInfo(highlighterWithInfo, lensManagerDispatcher::show, ::showAsynchronously)
	}
	
	private fun getFilteredHighlightInfo(highlighter: RangeHighlighter): HighlightInfo? {
		return HighlightInfo.fromRangeHighlighter(highlighter)?.takeIf { settings.severityFilter.test(it.severity) }
	}
	
	private inline fun processHighlighterWithInfo(highlighterWithInfo: HighlighterWithInfo, actionForImmediate: (HighlighterWithInfo) -> Unit, actionForAsync: (HighlighterWithInfo.Async) -> Unit) {
		if (highlighterWithInfo is HighlighterWithInfo.Async) {
			actionForAsync(highlighterWithInfo)
		}
		else if (highlighterWithInfo.hasDescription) {
			actionForImmediate(highlighterWithInfo)
		}
	}
	
	private fun showAsynchronously(highlighterWithInfo: HighlighterWithInfo.Async) {
		highlighterWithInfo.requestDescription {
			if (highlighterWithInfo.highlighter.isValid && highlighterWithInfo.hasDescription) {
				lensManagerDispatcher.show(highlighterWithInfo)
			}
		}
	}
	
	override fun beforeRemoved(highlighter: RangeHighlighterEx) {
		if (getFilteredHighlightInfo(highlighter) != null) {
			lensManagerDispatcher.hide(highlighter)
		}
	}
	
	fun hideAll() {
		lensManagerDispatcher.hideAll()
	}
}
