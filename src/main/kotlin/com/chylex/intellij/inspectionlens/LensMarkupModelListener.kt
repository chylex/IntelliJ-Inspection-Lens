package com.chylex.intellij.inspectionlens

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.rd.createNestedDisposable
import com.jetbrains.rd.util.lifetime.intersect

/**
 * Listens for inspection highlights and reports them to [EditorInlayLensManager].
 */
class LensMarkupModelListener private constructor(editor: Editor) : MarkupModelListener {
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
	
	private fun showIfValid(highlighter: RangeHighlighter) {
		runWithHighlighterIfValid(highlighter, lens::show, ::showAsynchronously)
	}
	
	private fun showAllValid(highlighters: Array<RangeHighlighter>) {
		val immediateHighlighters = mutableListOf<HighlighterWithInfo>()
		
		for (highlighter in highlighters) {
			runWithHighlighterIfValid(highlighter, immediateHighlighters::add, ::showAsynchronously)
		}
		
		lens.showAll(immediateHighlighters)
	}
	
	private fun showAsynchronously(highlighterWithInfo: HighlighterWithInfo.Async) {
		highlighterWithInfo.requestDescription {
			if (highlighterWithInfo.highlighter.isValid && highlighterWithInfo.hasDescription) {
				val application = ApplicationManager.getApplication()
				if (application.isDispatchThread) {
					lens.show(highlighterWithInfo)
				}
				else {
					application.invokeLater {
						lens.show(highlighterWithInfo)
					}
				}
			}
		}
	}

	companion object {
		// This is the same as adding one to the value of HighlightSeverity.TEXT
		// but HighlightSeverity.TEXT is not available in 2022.1
		private val MINIMUM_SEVERITY = HighlightSeverity.INFORMATION.myVal + 2
		
		private fun getHighlightInfoIfValid(highlighter: RangeHighlighter): HighlightInfo? {
			return if (highlighter.isValid)
				HighlightInfo.fromRangeHighlighter(highlighter)?.takeIf {it.severity.myVal >= MINIMUM_SEVERITY }
			else
				null
		}
		
		private inline fun runWithHighlighterIfValid(highlighter: RangeHighlighter, actionForImmediate: (HighlighterWithInfo) -> Unit, actionForAsync: (HighlighterWithInfo.Async) -> Unit) {
			val info = getHighlightInfoIfValid(highlighter)
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
		
		/**
		 * Attaches a new [LensMarkupModelListener] to the document model of the provided [TextEditor], and reports all existing inspection highlights to [EditorInlayLensManager].
		 * 
		 * The [LensMarkupModelListener] will be disposed when either the [TextEditor] is disposed, or via [InspectionLensPluginDisposableService] when the plugin is unloaded.
		 */
		fun install(textEditor: TextEditor) {
			val editor = textEditor.editor
			val markupModel = DocumentMarkupModel.forDocument(editor.document, editor.project, false)
			if (markupModel is MarkupModelEx) {
				val pluginLifetime = ApplicationManager.getApplication().getService(InspectionLensPluginDisposableService::class.java).createLifetime()
				val editorLifetime = textEditor.createLifetime()
				
				val listener = LensMarkupModelListener(editor)
				markupModel.addMarkupModelListener(pluginLifetime.intersect(editorLifetime).createNestedDisposable(), listener)
				listener.showAllValid(markupModel.allHighlighters)
			}
		}
	}
}

