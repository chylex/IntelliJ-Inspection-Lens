package com.chylex.intellij.inspectionlens

import com.chylex.intellij.inspectionlens.util.MultiParentDisposable
import com.intellij.codeInsight.daemon.impl.AsyncDescriptionSupplier
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
		if (!highlighter.isValid) {
			return
		}
		
		val info = HighlightInfo.fromRangeHighlighter(highlighter)
		if (info == null || info.severity.myVal < MINIMUM_SEVERITY) {
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
	
	private fun showIfNonNullDescription(highlighter: RangeHighlighter, info: HighlightInfo) {
		if (info.description != null) {
			lens.show(highlighter, info)
		}
	}
	
	companion object {
		private val MINIMUM_SEVERITY = HighlightSeverity.DEFAULT_SEVERITIES.toList().minusElement(HighlightSeverity.INFORMATION).minOf(HighlightSeverity::myVal)
		
		/**
		 * Attaches a new [LensMarkupModelListener] to the document model of the provided [TextEditor], and reports all existing inspection highlights to [EditorInlayLensManager].
		 * 
		 * The [LensMarkupModelListener] will be disposed when either the [TextEditor] is disposed, or via [InspectionLensPluginDisposableService] when the plugin is unloaded.
		 */
		fun install(textEditor: TextEditor) {
			val editor = textEditor.editor
			val markupModel = DocumentMarkupModel.forDocument(editor.document, editor.project, false)
			if (markupModel is MarkupModelEx) {
				val pluginDisposable = ApplicationManager.getApplication().getService(InspectionLensPluginDisposableService::class.java)
				
				val listenerDisposable = MultiParentDisposable()
				listenerDisposable.registerWithParent(textEditor)
				listenerDisposable.registerWithParent(pluginDisposable)
				
				val listener = LensMarkupModelListener(editor)
				markupModel.addMarkupModelListener(listenerDisposable.self, listener)
				
				for (highlighter in markupModel.allHighlighters) {
					listener.showIfValid(highlighter)
				}
			}
		}
	}
}
