package com.chylex.intellij.inspectionlens.editor

import com.chylex.intellij.inspectionlens.InspectionLensPluginDisposableService
import com.chylex.intellij.inspectionlens.utils.DebouncingInvokeOnDispatchThread
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
	
	private val showOnDispatchThread = DebouncingInvokeOnDispatchThread(lens::showAll)
	private val hideOnDispatchThread = DebouncingInvokeOnDispatchThread(lens::hideAll)
	
	override fun afterAdded(highlighter: RangeHighlighterEx) {
		showIfValid(highlighter)
	}
	
	override fun attributesChanged(highlighter: RangeHighlighterEx, renderersChanged: Boolean, fontStyleOrColorChanged: Boolean) {
		showIfValid(highlighter)
	}
	
	override fun beforeRemoved(highlighter: RangeHighlighterEx) {
		if (getFilteredHighlightInfo(highlighter) != null) {
			hideOnDispatchThread.enqueue(highlighter)
		}
	}
	
	private fun showIfValid(highlighter: RangeHighlighter) {
		runWithHighlighterIfValid(highlighter, showOnDispatchThread::enqueue, ::showAsynchronously)
	}
	
	private fun showAsynchronously(highlighterWithInfo: HighlighterWithInfo.Async) {
		highlighterWithInfo.requestDescription {
			if (highlighterWithInfo.highlighter.isValid && highlighterWithInfo.hasDescription) {
				showOnDispatchThread.enqueue(highlighterWithInfo)
			}
		}
	}
	
	companion object {
		private val MINIMUM_SEVERITY = HighlightSeverity.TEXT_ATTRIBUTES.myVal + 1
		
		private fun getFilteredHighlightInfo(highlighter: RangeHighlighter): HighlightInfo? {
			return HighlightInfo.fromRangeHighlighter(highlighter)?.takeIf { it.severity.myVal >= MINIMUM_SEVERITY }
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
				
				for (highlighter in markupModel.allHighlighters) {
					listener.showIfValid(highlighter)
				}
			}
		}
	}
}
