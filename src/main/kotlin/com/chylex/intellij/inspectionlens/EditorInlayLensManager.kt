package com.chylex.intellij.inspectionlens

import com.chylex.intellij.inspectionlens.EditorInlayLensManager.Companion.MAXIMUM_SEVERITY
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
		
		/**
		 * Highest allowed severity for the purposes of sorting multiple highlights at the same offset.
		 * A [MAXIMUM_SEVERITY] of 500 allows for 8 589 933 positions in the document before sorting breaks down.
		 * The value is a little higher than the highest [com.intellij.lang.annotation.HighlightSeverity], in case severities with higher values are introduced in the future.
		 */
		private const val MAXIMUM_SEVERITY = 500
		
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
		
		internal fun getInlayHintPriority(offset: Int, severity: Int): Int {
			// Sorts highlights first by offset in the document, then by severity.
			val positionBucket = offset.coerceAtLeast(0) * MAXIMUM_SEVERITY.toLong()
			val positionFactor = (Int.MAX_VALUE - MAXIMUM_SEVERITY - positionBucket).coerceAtLeast(Int.MIN_VALUE + 1L).toInt()
			val severityFactor = severity.coerceIn(0, MAXIMUM_SEVERITY)
			// The result is between (Int.MIN_VALUE + 1)..Int.MAX_VALUE, allowing for negation without overflow.
			return positionFactor + severityFactor
		}
		
		private fun getInlayHintPriority(info: HighlightInfo): Int {
			return getInlayHintPriority(info.actualEndOffset, info.severity.myVal)
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
			val priority = getInlayHintPriority(info)
			val renderer = LensRenderer(info)
			val properties = InlayProperties().relatesToPrecedingText(true).disableSoftWrapping(true).priority(priority)
			
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
