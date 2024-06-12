package com.chylex.intellij.inspectionlens.editor

import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key

/**
 * Listens for inspection highlights and reports them to [EditorLensManager].
 */
internal class LensMarkupModelListener private constructor(editor: Editor) : MarkupModelListener {
	private val lensManagerDispatcher = EditorLensManagerDispatcher(EditorLensManager.getOrCreate(editor))
	
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
	
	private fun showAllValid(highlighters: Array<RangeHighlighter>) {
		highlighters.forEach(::showIfValid)
	}
	
	private fun hideAll() {
		lensManagerDispatcher.hideAll()
	}
	
	companion object {
		private val EDITOR_KEY = Key<LensMarkupModelListener>(LensMarkupModelListener::class.java.name)
		private val SETTINGS_SERVICE = service<LensSettingsState>()
		
		private fun getFilteredHighlightInfo(highlighter: RangeHighlighter): HighlightInfo? {
			return HighlightInfo.fromRangeHighlighter(highlighter)?.takeIf { SETTINGS_SERVICE.severityFilter.test(it.severity) }
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
		
		private fun getMarkupModel(editor: Editor): MarkupModelEx? {
			return DocumentMarkupModel.forDocument(editor.document, editor.project, false) as? MarkupModelEx
		}
		
		/**
		 * Attaches a new [LensMarkupModelListener] to the [Editor], and reports all existing inspection highlights to [EditorLensManager].
		 */
		fun register(editor: Editor, disposable: Disposable) {
			if (editor.getUserData(EDITOR_KEY) != null) {
				return
			}
			
			val markupModel = getMarkupModel(editor) ?: return
			val listener = LensMarkupModelListener(editor)
			
			editor.putUserData(EDITOR_KEY, listener)
			Disposer.register(disposable) { editor.putUserData(EDITOR_KEY, null) }
			
			markupModel.addMarkupModelListener(disposable, listener)
			listener.showAllValid(markupModel.allHighlighters)
		}
		
		/**
		 * Recreates all inspection highlights in the [Editor].
		 */
		fun refresh(editor: Editor) {
			val listener = editor.getUserData(EDITOR_KEY) ?: return
			val markupModel = getMarkupModel(editor) ?: return
			
			listener.hideAll()
			listener.showAllValid(markupModel.allHighlighters)
		}
	}
}
