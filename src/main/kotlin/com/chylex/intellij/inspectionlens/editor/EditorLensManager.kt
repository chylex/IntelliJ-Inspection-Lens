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
		
		if (!highlighter.isValid) {
			return
		}
		
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
	
	private fun hide(highlighter: RangeHighlighter) {
		lenses.remove(highlighter)?.hide()
	}
	
	fun hideAll() {
		executeInBatchMode(lenses.size) {
			lenses.values.forEach(EditorLens::hide)
			lenses.clear()
		}
	}
	
	fun execute(commands: Collection<Command>) {
		executeInBatchMode(commands.size) {
			commands.forEach { it.apply(this) }
		}
	}
	
	sealed interface Command {
		fun apply(lensManager: EditorLensManager)
		
		class Show(private val highlighter: HighlighterWithInfo) : Command {
			override fun apply(lensManager: EditorLensManager) {
				lensManager.show(highlighter)
			}
		}
		
		class Hide(private val highlighter: RangeHighlighter) : Command {
			override fun apply(lensManager: EditorLensManager) {
				lensManager.hide(highlighter)
			}
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
