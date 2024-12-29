package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import java.awt.Color
import java.util.EnumMap

internal enum class ColorMode {
	AUTO,
	ALWAYS_LIGHT,
	ALWAYS_DARK;
	
	companion object {
		private val KEY = Key.create<ColorMode>("InspectionLens.ColorMode")
		
		fun getFromEditor(editor: Editor): ColorMode {
			return KEY.get(editor, AUTO)
		}
		
		fun setForEditor(editor: Editor, colorMode: ColorMode) {
			KEY.set(editor, colorMode)
		}
		
		inline fun createColorAttributes(lightThemeColor: Color, darkThemeColor: Color, attributesFactory: (JBColor) -> LensSeverityTextAttributes): EnumMap<ColorMode, LensSeverityTextAttributes> {
			val result = EnumMap<ColorMode, LensSeverityTextAttributes>(ColorMode::class.java)
			
			result[AUTO] = attributesFactory(JBColor(lightThemeColor, darkThemeColor))
			result[ALWAYS_LIGHT] = attributesFactory(JBColor(lightThemeColor, lightThemeColor))
			result[ALWAYS_DARK] = attributesFactory(JBColor(darkThemeColor, darkThemeColor))
			
			return result
		}
	}
}
