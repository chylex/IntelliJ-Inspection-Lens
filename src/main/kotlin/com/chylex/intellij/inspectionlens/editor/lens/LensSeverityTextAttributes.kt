package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.openapi.editor.markup.UnmodifiableTextAttributes
import com.intellij.ui.JBColor
import java.awt.Font

class LensSeverityTextAttributes(
	private val foregroundColor: JBColor? = null,
	private val backgroundColor: JBColor? = null,
	private val fontStyle: Int = Font.PLAIN,
) : UnmodifiableTextAttributes() {
	override fun getForegroundColor(): JBColor? {
		return foregroundColor
	}
	
	override fun getBackgroundColor(): JBColor? {
		return backgroundColor
	}
	
	override fun getFontType(): Int {
		return fontStyle
	}
}
