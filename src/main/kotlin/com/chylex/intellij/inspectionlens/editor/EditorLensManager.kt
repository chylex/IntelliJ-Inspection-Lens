package com.chylex.intellij.inspectionlens.editor

import com.chylex.intellij.inspectionlens.editor.lens.EditorLens
import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import java.util.IdentityHashMap

/**
 * Manages visible inspection lenses for an [Editor].
 */
internal class EditorLensManager(private val editor: Editor) {
	private val lenses = IdentityHashMap<RangeHighlighter, EditorLens>()
	private val settings = service<LensSettingsState>()
	
	private fun show(highlighterWithInfo: HighlighterWithInfo) {
		val (highlighter, info) = highlighterWithInfo
		
		if (!highlighter.isValid) {
			return
		}
		
		val existingLens = lenses[highlighter]
		if (existingLens != null) {
			if (existingLens.update(info, settings)) {
				return
			}
			
			existingLens.hide()
		}
		
		val newLens = EditorLens.show(editor, info, settings)
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
		
		object HideAll : Command {
			override fun apply(lensManager: EditorLensManager) {
				lensManager.hideAll()
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
