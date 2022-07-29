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
		
		private fun getInlayHintOffset(info: HighlightInfo): Int {
			// Ensures a highlight at the end of a line does not overflow to the next line.
			return info.actualEndOffset - 1
		}
	}
	
	private val inlays = mutableMapOf<RangeHighlighter, Inlay<LensRenderer>>()
	
	fun show(highlighterWithInfo: HighlighterWithInfo) {
		val (highlighter, info) = highlighterWithInfo
		val currentInlay = inlays[highlighter]
		if (currentInlay != null && currentInlay.isValid) {
			currentInlay.renderer.setPropertiesFrom(info)
			currentInlay.update()
		}
		else {
			val offset = getInlayHintOffset(info)
			val renderer = LensRenderer(info)
			val properties = InlayProperties().relatesToPrecedingText(true).disableSoftWrapping(true).priority(-offset)
			
			editor.inlayModel.addAfterLineEndElement(offset, properties, renderer)?.let {
				inlays[highlighter] = it
			}
		}
	}
	
	fun showAll(highlightersWithInfo: Collection<HighlighterWithInfo>) {
		executeInInlayBatchMode(highlightersWithInfo.size) { highlightersWithInfo.forEach(::show) }
	}
	
	fun hide(highlighter: RangeHighlighter) {
		inlays.remove(highlighter)?.dispose()
	}
	
	fun hideAll() {
		executeInInlayBatchMode(inlays.size) { inlays.values.forEach(Inlay<*>::dispose) }
		inlays.clear()
	}
	
	private fun executeInInlayBatchMode(operations: Int, block: () -> Unit) {
		editor.inlayModel.execute(operations > 1000, block)
	}
}
