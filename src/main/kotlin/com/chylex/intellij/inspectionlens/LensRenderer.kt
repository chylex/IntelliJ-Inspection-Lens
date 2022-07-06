package com.chylex.intellij.inspectionlens

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Renders the text of an inspection lens.
 */
class LensRenderer : HintRenderer(null) {
	private companion object {
		private val ATTRIBUTES = TextAttributes(null, null, null, null, Font.ITALIC)
	}
	
	var severity = LensSeverity.OTHER
	
	override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
		r.y += 1
		super.paint(inlay, g, r, textAttributes)
	}
	
	override fun getTextAttributes(editor: Editor): TextAttributes {
		return ATTRIBUTES.also { it.foregroundColor = severity.getColor(editor) }
	}
	
	override fun useEditorFont(): Boolean {
		return true
	}
}
