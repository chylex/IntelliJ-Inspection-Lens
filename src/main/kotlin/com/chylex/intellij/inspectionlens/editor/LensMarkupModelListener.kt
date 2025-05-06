package com.chylex.intellij.inspectionlens.editor

import com.chylex.intellij.inspectionlens.InspectionLens
import com.chylex.intellij.inspectionlens.debug.Highlighter
import com.chylex.intellij.inspectionlens.debug.LensEventData
import com.chylex.intellij.inspectionlens.debug.LensEventManager
import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.editor.markup.RangeHighlighter

/**
 * Listens for inspection highlights and reports them to [EditorLensManager].
 */
internal class LensMarkupModelListener(private val editor: Editor, private val lensManagerDispatcher: EditorLensManagerDispatcher) : MarkupModelListener {
	private val settings = service<LensSettingsState>()
	
	override fun afterAdded(highlighter: RangeHighlighterEx) {
		try {
			getFilteredHighlightInfo(highlighter)?.let { LensEventManager.addEvent(editor, LensEventData.MarkupModelAfterAdded(Highlighter(highlighter, it))) }
			showIfValid(highlighter)
		} catch (e: Exception) {
			InspectionLens.LOG.error("Error showing inspection", e)
		}
	}
	
	override fun attributesChanged(highlighter: RangeHighlighterEx, renderersChanged: Boolean, fontStyleOrColorChanged: Boolean) {
		try {
			getFilteredHighlightInfo(highlighter)?.let { LensEventManager.addEvent(editor, LensEventData.MarkupModelAttributesChanged(Highlighter(highlighter, it))) }
			showIfValid(highlighter)
		} catch (e: Exception) {
			InspectionLens.LOG.error("Error updating inspection", e)
		}
	}
	
	override fun beforeRemoved(highlighter: RangeHighlighterEx) {
		try {
			val filteredHighlightInfo = getFilteredHighlightInfo(highlighter)
			if (filteredHighlightInfo != null) {
				LensEventManager.addEvent(editor, LensEventData.MarkupModelBeforeRemoved(Highlighter(highlighter, filteredHighlightInfo)))
				lensManagerDispatcher.hide(highlighter)
			}
		} catch (e: Exception) {
			InspectionLens.LOG.error("Error hiding inspection", e)
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
		if (info != null) {
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
