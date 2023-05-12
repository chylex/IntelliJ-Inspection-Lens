package com.chylex.intellij.inspectionlens.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import java.util.IdentityHashMap

/**
 * Manages visible inspection lenses for an [Editor].
 */
class EditorLensManager private constructor(private val editor: Editor) {
	companion object {
		private val EDITOR_KEY = Key<EditorLensManager>(EditorLensManager::class.java.name)
		
		fun getOrCreate(editor: Editor): EditorLensManager {
			return editor.getUserData(EDITOR_KEY) ?: EditorLensManager(editor).also { editor.putUserData(EDITOR_KEY, it) }
		}
		
		fun remove(editor: Editor) {
			val manager = editor.getUserData(EDITOR_KEY)
			if (manager != null) {
				manager.hideAll()
				editor.putUserData(EDITOR_KEY, null)
			}
		}
	}
	
	private val lenses = IdentityHashMap<RangeHighlighter, EditorLens>()
	
	private fun show(highlighterWithInfo: HighlighterWithInfo) {
		val (highlighter, info) = highlighterWithInfo
		
		val existingLens = lenses[highlighter]
		if (existingLens != null) {
			if (existingLens.update(info)) {
				return
			}
			
			existingLens.hide()
		}
		
		val newLens = EditorLens.show(editor, info)
		if (newLens != null) {
			lenses[highlighter] = newLens
		}
		else if (existingLens != null) {
			lenses.remove(highlighter)
		}
	}
	
	fun show(highlightersWithInfo: Collection<HighlighterWithInfo>) {
		executeInBatchMode(highlightersWithInfo.size) {
			highlightersWithInfo.forEach(::show)
		}
	}
	
	private fun hide(highlighter: RangeHighlighter) {
		lenses.remove(highlighter)?.hide()
	}
	
	fun hide(highlighters: Collection<RangeHighlighter>) {
		executeInBatchMode(highlighters.size) {
			highlighters.forEach(::hide)
		}
	}
	
	fun hideAll() {
		executeInBatchMode(lenses.size) {
			lenses.values.forEach(EditorLens::hide)
			lenses.clear()
		}
	}
	
	/**
	 * Batch mode affects both inlays and highlighters used for line colors.
	 */
	@Suppress("ConvertLambdaToReference")
	private inline fun executeInBatchMode(operations: Int, crossinline action: () -> Unit) {
		if (operations > 1000) {
			editor.inlayModel.execute(true) { action() }
		}
		else {
			action()
		}
	}
}
