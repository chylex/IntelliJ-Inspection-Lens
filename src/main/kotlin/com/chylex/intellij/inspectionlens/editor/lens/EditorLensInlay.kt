package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties

@JvmInline
internal value class EditorLensInlay(private val inlay: Inlay<LensRenderer>) {
	val editor
		get() = inlay.editor
	
	fun tryUpdate(info: HighlightInfo): Boolean {
		if (!inlay.isValid) {
			return false
		}
		
		inlay.renderer.setPropertiesFrom(info)
		inlay.update()
		return true
	}
	
	fun hide() {
		inlay.dispose()
	}
	
	companion object {
		fun show(editor: Editor, info: HighlightInfo, settings: LensSettingsState): EditorLensInlay? {
			val offset = getInlayHintOffset(info)
			val priority = getInlayHintPriority(editor, info)
			val renderer = LensRenderer(info, settings)
			
			val properties = InlayProperties()
				.relatesToPrecedingText(true)
				.disableSoftWrapping(true)
				.priority(priority)
			
			return editor.inlayModel.addAfterLineEndElement(offset, properties, renderer)
				?.also(renderer::setInlay)
				?.let(::EditorLensInlay)
		}
		
		/**
		 * Highest allowed severity for the purposes of sorting multiple highlights at the same offset.
		 * The value is a little higher than the highest [com.intellij.lang.annotation.HighlightSeverity], in case severities with higher values are introduced in the future.
		 */
		private const val MAXIMUM_SEVERITY = 500
		private const val MAXIMUM_POSITION = ((Int.MAX_VALUE / MAXIMUM_SEVERITY) * 2) - 1
		
		private fun getInlayHintOffset(info: HighlightInfo): Int {
			// Ensures a highlight at the end of a line does not overflow to the next line.
			return info.actualEndOffset - 1
		}
		
		fun getInlayHintPriority(position: Int, severity: Int): Int {
			// Sorts highlights first by position on the line, then by severity.
			val positionBucket = position.coerceIn(0, MAXIMUM_POSITION) * MAXIMUM_SEVERITY
			// The multiplication can overflow, but subtracting overflowed result from Int.MAX_VALUE does not break continuity.
			val positionFactor = Integer.MAX_VALUE - positionBucket
			val severityFactor = severity.coerceIn(0, MAXIMUM_SEVERITY) - MAXIMUM_SEVERITY
			return positionFactor + severityFactor
		}
		
		private fun getInlayHintPriority(editor: Editor, info: HighlightInfo): Int {
			val startOffset = info.actualStartOffset
			val positionOnLine = startOffset - getLineStartOffset(editor, startOffset)
			return getInlayHintPriority(positionOnLine, info.severity.myVal)
		}
		
		private fun getLineStartOffset(editor: Editor, offset: Int): Int {
			val position = editor.offsetToLogicalPosition(offset)
			return editor.document.getLineStartOffset(position.line)
		}	
	}
}
