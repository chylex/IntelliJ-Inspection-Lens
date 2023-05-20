package com.chylex.intellij.inspectionlens.editor

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

/**
 * Determines properties of inspection lenses based on severity.
 */
@Suppress("UseJBColor", "InspectionUsingGrayColors")
enum class LensSeverity(baseColor: Color, lightThemeDarkening: Int, darkThemeBrightening: Int) {
	ERROR          (Color(158,  41,  39), lightThemeDarkening = 1, darkThemeBrightening = 4),
	WARNING        (Color(190, 145,  23), lightThemeDarkening = 4, darkThemeBrightening = 1),
	WEAK_WARNING   (Color(117, 109,  86), lightThemeDarkening = 3, darkThemeBrightening = 3),
	SERVER_PROBLEM (Color(176,  97,   0), lightThemeDarkening = 4, darkThemeBrightening = 2),
	OTHER          (Color(128, 128, 128), lightThemeDarkening = 1, darkThemeBrightening = 2);
	
	val colorAttributes: LensSeverityTextAttributes
	
	init {
		val lightThemeColor = ColorUtil.saturate(ColorUtil.darker(baseColor, lightThemeDarkening), 1)
		val darkThemeColor = ColorUtil.desaturate(ColorUtil.brighter(baseColor, darkThemeBrightening), 2)
		
		val textColor = JBColor(lightThemeColor, darkThemeColor)
		colorAttributes = LensSeverityTextAttributes(foregroundColor = textColor, fontStyle = Font.ITALIC)
	}
	
	companion object {
		fun from(severity: HighlightSeverity) = when (severity) {
			HighlightSeverity.ERROR                           -> ERROR
			HighlightSeverity.WARNING                         -> WARNING
			HighlightSeverity.WEAK_WARNING                    -> WEAK_WARNING
			HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING -> SERVER_PROBLEM
			else                                              -> OTHER
		}
	}
}
