package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.InspectionLens
import com.chylex.intellij.inspectionlens.compatibility.SpellCheckerSupport
import com.chylex.intellij.inspectionlens.editor.lens.ColorMode.Companion.createColorAttributes
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.ui.ColorUtil
import com.intellij.ui.ColorUtil.toAlpha
import java.awt.Color
import java.awt.Font
import java.util.Collections
import java.util.EnumMap

/**
 * Determines properties of inspection lenses based on severity.
 */
@Suppress("UseJBColor", "InspectionUsingGrayColors")
enum class LensSeverity(private val baseColor: Color, private val lightThemeDarkening: Int, private val darkThemeBrightening: Int) {
	ERROR          (Color(158,  41,  39), lightThemeDarkening = 2, darkThemeBrightening = 4),
	WARNING        (Color(190, 145,  23), lightThemeDarkening = 5, darkThemeBrightening = 1),
	WEAK_WARNING   (Color(117, 109,  86), lightThemeDarkening = 4, darkThemeBrightening = 4),
	SERVER_PROBLEM (Color(176,  97,   0), lightThemeDarkening = 5, darkThemeBrightening = 2),
	GRAZIE         (Color( 53, 146, 196), lightThemeDarkening = 3, darkThemeBrightening = 1),
	TYPO           (Color( 73, 156,  84), lightThemeDarkening = 4, darkThemeBrightening = 1),
	OTHER          (Color(128, 128, 128), lightThemeDarkening = 2, darkThemeBrightening = 2);
	
	private lateinit var textAttributes: EnumMap<ColorMode, LensSeverityTextAttributes>
	private lateinit var lineAttributes: EnumMap<ColorMode, LensSeverityTextAttributes>
	
	init {
		refreshColors()
	}
	
	internal fun refreshColors() {
		val theme = if (ColorGenerator.useGenerator) {
			ColorGenerator.generate(name, baseColor)
		}
		else {
			val lightThemeTextColor = ColorUtil.saturate(ColorUtil.darker(baseColor, lightThemeDarkening), 1)
			val darkThemeTextColor = ColorUtil.desaturate(ColorUtil.brighter(baseColor, darkThemeBrightening), 2)
			
			val lightThemeLineColor = toAlpha(lightThemeTextColor, 10)
			val darkThemeLineColor = toAlpha(darkThemeTextColor, 13)
			
			ColorGenerator.Theme(lightThemeTextColor, lightThemeLineColor, darkThemeTextColor, darkThemeLineColor)
		}
		
		textAttributes = createColorAttributes(theme.lightThemeTextColor, theme.darkThemeTextColor) { LensSeverityTextAttributes(foregroundColor = it, fontStyle = Font.ITALIC) }
		lineAttributes = createColorAttributes(theme.lightThemeLineColor, theme.darkThemeLineColor) { LensSeverityTextAttributes(backgroundColor = it) }
	}
	
	internal fun getTextAttributes(colorMode: ColorMode): LensSeverityTextAttributes {
		return textAttributes.getValue(colorMode)
	}
	
	internal fun getLineAttributes(colorMode: ColorMode): LensSeverityTextAttributes {
		return lineAttributes.getValue(colorMode)
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
