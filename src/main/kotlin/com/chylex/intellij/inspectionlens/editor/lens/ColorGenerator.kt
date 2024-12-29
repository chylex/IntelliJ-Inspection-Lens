package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.ui.ColorUtil
import java.awt.Color
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

internal object ColorGenerator {
	private const val HACK_BRIGHTNESS_STEP = 1.025F
	
	var useGenerator = false
	var lightThemeDesaturation = 1
	var lightThemeThreshold = 32
	var lightThemeLineAlpha = 10
	var darkThemeDesaturation = 2
	var darkThemeThreshold = 62
	var darkThemeLineAlpha = 13
	
	data class Theme(
		val lightThemeTextColor: Color,
		val lightThemeLineColor: Color,
		val darkThemeTextColor: Color,
		val darkThemeLineColor: Color,
	)

	fun generate(name: String, baseColor: Color): Theme {
		val lightThemeTextColor: Color = findColorWithPerceivedBrightness("$name / Light", baseColor, lightThemeDesaturation, 50 downTo -50) { it <= lightThemeThreshold }
		val darkThemeTextColor: Color = findColorWithPerceivedBrightness("$name / Dark", baseColor, darkThemeDesaturation, -50..50) { it >= darkThemeThreshold }
		
		val lightThemeLineColor = ColorUtil.toAlpha(lightThemeTextColor, lightThemeLineAlpha)
		val darkThemeLineColor = ColorUtil.toAlpha(darkThemeTextColor, darkThemeLineAlpha)
		
		return Theme(lightThemeTextColor, lightThemeLineColor, darkThemeTextColor, darkThemeLineColor)
	}
	
	private fun findColorWithPerceivedBrightness(name: String, baseColor: Color, desaturation: Int, steps: IntProgression, testPerceivedBrightness: (Double) -> Boolean): Color {
		var finalColor = baseColor
		var finalBrightening = 0
		var finalPerceivedBrightness = 0.0
		
		for (brightening in steps) {
			finalColor = ColorUtil.desaturate(ColorUtil.hackBrightness(baseColor, abs(brightening), if (brightening < 0) 1F / HACK_BRIGHTNESS_STEP else HACK_BRIGHTNESS_STEP), desaturation)
			finalBrightening = brightening
			finalPerceivedBrightness = getPerceivedBrightness(finalColor)
			
			if (testPerceivedBrightness(finalPerceivedBrightness)) {
				break
			}
		}
		
		println("$name - ${baseColor.red},${baseColor.green},${baseColor.blue} --> step $finalBrightening; brightness ${String.format(Locale.ROOT, "%.2f", finalPerceivedBrightness)}; color ${finalColor.red},${finalColor.green},${finalColor.blue}")
		return finalColor
	}
	
	private fun getLuminance(r: Double, g: Double, b: Double): Double {
		return 0.2126 * srgbToLinearRgb(r) + 0.7152 * srgbToLinearRgb(g) + 0.0722 * srgbToLinearRgb(b)
	}
	
	private fun srgbToLinearRgb(channel: Double): Double {
		return if (channel <= 0.04045)
			channel / 12.92
		else
			((channel + 0.055) / 1.055).pow(2.4)
	}
	
	private fun getPerceivedBrightness(luminance: Double): Double {
		return if (luminance <= (216.0 / 24389.0))
			luminance * (24389.0 / 27.0)
		else
			luminance.pow(1.0 / 3.0) * 116 - 16
	}
	
	private fun getPerceivedBrightness(color: Color): Double {
		val r = color.red / 255.0
		val g = color.green / 255.0
		val b = color.blue / 255.0
		return getPerceivedBrightness(getLuminance(r, g, b))
	}
}
