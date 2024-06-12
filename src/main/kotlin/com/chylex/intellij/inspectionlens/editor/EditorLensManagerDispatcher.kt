package com.chylex.intellij.inspectionlens.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.RangeHighlighter

class EditorLensManagerDispatcher(private val lensManager: EditorLensManager) {
	private var queuedItems = mutableListOf<EditorLensManager.Command>()
	private var isEnqueued = false
	
	fun show(highlighterWithInfo: HighlighterWithInfo) {
		enqueue(EditorLensManager.Command.Show(highlighterWithInfo))
	}
	
	fun hide(highlighter: RangeHighlighter) {
		enqueue(EditorLensManager.Command.Hide(highlighter))
	}
	
	fun hideAll() {
		enqueue(EditorLensManager.Command.HideAll)
	}
	
	private fun enqueue(item: EditorLensManager.Command) {
		synchronized(this) {
			queuedItems.add(item)
			
			// Enqueue even if already on dispatch thread to debounce consecutive calls.
			if (!isEnqueued) {
				isEnqueued = true
				ApplicationManager.getApplication().invokeLater(::process)
			}
		}
	}
	
	private fun process() {
		var itemsToProcess: List<EditorLensManager.Command>
		
		synchronized(this) {
			itemsToProcess = queuedItems
			queuedItems = mutableListOf()
			isEnqueued = false
		}
		
		lensManager.execute(itemsToProcess)
	}
}
