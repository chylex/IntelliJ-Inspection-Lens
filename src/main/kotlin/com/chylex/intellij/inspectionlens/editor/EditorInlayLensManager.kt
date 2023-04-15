package com.chylex.intellij.inspectionlens.editor

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
		 * The value is a little higher than the highest [com.intellij.lang.annotation.HighlightSeverity], in case severities with higher values are introduced in the future.
		 */
		private const val MAXIMUM_SEVERITY = 500
		private const val MAXIMUM_POSITION = ((Int.MAX_VALUE / MAXIMUM_SEVERITY) * 2) - 1
		
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
		
		internal fun getInlayHintPriority(position: Int, severity: Int): Int {
			// Sorts highlights first by position on the line, then by severity.
			val positionBucket = position.coerceIn(0, MAXIMUM_POSITION) * MAXIMUM_SEVERITY
			// The multiplication can overflow, but subtracting overflowed result from Int.MAX_VALUE does not break continuity.
			val positionFactor = Integer.MAX_VALUE - positionBucket
			val severityFactor = severity.coerceIn(0, MAXIMUM_SEVERITY) - MAXIMUM_SEVERITY
			return positionFactor + severityFactor
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
	
	private fun getInlayHintPriority(info: HighlightInfo): Int {
		val startOffset = info.actualStartOffset
		val positionOnLine = startOffset - getLineStartOffset(startOffset)
		return getInlayHintPriority(positionOnLine, info.severity.myVal)
	}
	
	private fun getLineStartOffset(offset: Int): Int {
		val position = editor.offsetToLogicalPosition(offset)
		return editor.document.getLineStartOffset(position.line)
	}
	
	private fun executeInInlayBatchMode(operations: Int, block: () -> Unit) {
		editor.inlayModel.execute(operations > 1000, block)
	}
}
