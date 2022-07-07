package com.chylex.intellij.inspectionlens

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key

/**
 * Manages visible inspection lenses for an [Editor].
 */
class EditorInlayLensManager private constructor(private val editor: Editor) {
	companion object {
		private val KEY = Key<EditorInlayLensManager>(EditorInlayLensManager::class.java.name)
		
		fun getOrCreate(editor: Editor): EditorInlayLensManager {
			return editor.getUserData(KEY) ?: EditorInlayLensManager(editor).also { editor.putUserData(KEY, it) }
		}
		
		fun remove(editor: Editor) {
			val manager = editor.getUserData(KEY)
			if (manager != null) {
				manager.hideAll()
				editor.putUserData(KEY, null)
			}
		}
		
		private fun updateRenderer(renderer: LensRenderer, info: HighlightInfo) {
			renderer.text = info.description.takeIf(String::isNotBlank)?.let(::addMissingPeriod) ?: " "
			renderer.severity = LensSeverity.from(info.severity)
		}
		
		private fun addMissingPeriod(text: String): String {
			return if (text.endsWith('.')) text else "$text."
		}
	}
	
	private val inlays = mutableMapOf<RangeHighlighter, Inlay<LensRenderer>>()
	
	fun show(highlighter: RangeHighlighter, info: HighlightInfo) {
		val currentInlay = inlays[highlighter]
		if (currentInlay != null && currentInlay.isValid) {
			updateRenderer(currentInlay.renderer, info)
			currentInlay.update()
		}
		else {
			val offset = info.actualEndOffset - 1
			val renderer = LensRenderer().also { updateRenderer(it, info) }
			val properties = InlayProperties().relatesToPrecedingText(true).priority(-offset)
			
			editor.inlayModel.addAfterLineEndElement(offset, properties, renderer)?.let {
				inlays[highlighter] = it
			}
		}
	}
	
	fun hide(highlighter: RangeHighlighter) {
		inlays.remove(highlighter)?.dispose()
	}
	
	fun hideAll() {
		inlays.values.forEach(Inlay<*>::dispose)
		inlays.clear()
	}
}
