package com.chylex.intellij.inspectionlens.editor

import com.intellij.openapi.editor.markup.UnmodifiableTextAttributes
import com.intellij.ui.JBColor
import java.awt.Font

class LensSeverityTextAttributes(private val foregroundColor: JBColor, private val fontStyle: Int = Font.PLAIN) : UnmodifiableTextAttributes() {
	override fun getForegroundColor(): JBColor {
		return foregroundColor
	}
	
	override fun getFontType(): Int {
		return fontStyle
	}
}
