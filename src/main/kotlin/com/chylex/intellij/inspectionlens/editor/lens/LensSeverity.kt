package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.InspectionLens
import com.chylex.intellij.inspectionlens.compatibility.SpellCheckerSupport
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.ui.ColorUtil
import com.intellij.ui.ColorUtil.toAlpha
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.util.Collections

/**
 * Determines properties of inspection lenses based on severity.
 */
@Suppress("UseJBColor", "InspectionUsingGrayColors")
enum class LensSeverity(baseColor: Color, lightThemeDarkening: Int, darkThemeBrightening: Int) {
	ERROR          (Color(158,  41,  39), lightThemeDarkening = 2, darkThemeBrightening = 4),
	WARNING        (Color(190, 145,  23), lightThemeDarkening = 5, darkThemeBrightening = 1),
	WEAK_WARNING   (Color(117, 109,  86), lightThemeDarkening = 4, darkThemeBrightening = 4),
	SERVER_PROBLEM (Color(176,  97,   0), lightThemeDarkening = 5, darkThemeBrightening = 2),
	GRAZIE         (Color( 53, 146, 196), lightThemeDarkening = 3, darkThemeBrightening = 1),
	TYPO           (Color( 73, 156,  84), lightThemeDarkening = 4, darkThemeBrightening = 1),
	OTHER          (Color(128, 128, 128), lightThemeDarkening = 2, darkThemeBrightening = 2);
	
	val textAttributes: LensSeverityTextAttributes
	val lineAttributes: LensSeverityTextAttributes
	
	init {
		val lightThemeColor = ColorUtil.saturate(ColorUtil.darker(baseColor, lightThemeDarkening), 1)
		val darkThemeColor = ColorUtil.desaturate(ColorUtil.brighter(baseColor, darkThemeBrightening), 2)
		
		val textColor = JBColor(lightThemeColor, darkThemeColor)
		val lineColor = JBColor(toAlpha(lightThemeColor, 10), toAlpha(darkThemeColor, 13))
		
		textAttributes = LensSeverityTextAttributes(foregroundColor = textColor, fontStyle = Font.ITALIC)
		lineAttributes = LensSeverityTextAttributes(backgroundColor = lineColor)
	}
	
	companion object {
		private val mapping = Collections.synchronizedMap(mapOf(
			HighlightSeverity.ERROR                           to ERROR,
			HighlightSeverity.WARNING                         to WARNING,
			HighlightSeverity.WEAK_WARNING                    to WEAK_WARNING,
			HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING to SERVER_PROBLEM,
		))
		
		init {
			SpellCheckerSupport.load()
		}
		
		/**
		 * Registers a mapping from a [HighlightSeverity] to a [LensSeverity], and refreshes all open editors.
		 */
		internal fun registerMapping(severity: HighlightSeverity, lensSeverity: LensSeverity) {
			if (mapping.put(severity, lensSeverity) != lensSeverity) {
				InspectionLens.scheduleRefresh()
			}
		}
		
		/**
		 * Returns the [LensSeverity] associated with the [HighlightSeverity], or [OTHER] if no explicit mapping is found.
		 */
		fun from(severity: HighlightSeverity): LensSeverity {
			return mapping.getOrDefault(severity, OTHER)
		}
	}
}
