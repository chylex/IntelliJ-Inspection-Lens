package com.chylex.intellij.inspectionlens

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.ui.ColorUtil
import java.awt.Color

/**
 * Determines properties of inspection lenses based on severity.
 */
enum class LensSeverity(color: Color, darkThemeBrightening: Int, lightThemeDarkening: Int) {
	ERROR(
		Color(158, 41, 39),
		darkThemeBrightening = 4,
		lightThemeDarkening = 1,
	),
	
	WARNING(
		Color(190, 145, 23),
		darkThemeBrightening = 1,
		lightThemeDarkening = 4,
	),
	
	WEAK_WARNING(
		Color(117, 109, 86),
		darkThemeBrightening = 3,
		lightThemeDarkening = 3,
	),
	
	SERVER_PROBLEM(
		Color(176, 97, 0),
		darkThemeBrightening = 2,
		lightThemeDarkening = 4,
	),
	
	OTHER(
		Color(128, 128, 128),
		darkThemeBrightening = 2,
		lightThemeDarkening = 1,
	);
	
	private val darkThemeColor = ColorUtil.desaturate(ColorUtil.brighter(color, darkThemeBrightening), 2)
	private val lightThemeColor = ColorUtil.saturate(ColorUtil.darker(color, lightThemeDarkening), 1)
	
	fun getColor(editor: Editor): Color {
		val isDarkTheme = ColorUtil.isDark(editor.colorsScheme.defaultBackground)
		return if (isDarkTheme) darkThemeColor else lightThemeColor
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
