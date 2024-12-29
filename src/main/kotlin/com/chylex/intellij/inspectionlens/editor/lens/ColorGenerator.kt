package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.ui.ColorUtil
import java.awt.Color
import kotlin.math.pow

internal object ColorGenerator {
	private const val HACK_BRIGHTNESS_STEP = 1.05F
	
	var useGenerator = false
	var lightThemeDesaturation = 1
	var darkThemeDesaturation = 2
	var lightThemeThreshold = 32
	var darkThemeThreshold = 62

	fun generate(baseColor: Color): Pair<Color, Color> {
		var lightThemeColor = baseColor
		var darkThemeColor = baseColor
		
		for (darkening in 0..10) {
			lightThemeColor = ColorUtil.desaturate(ColorUtil.hackBrightness(baseColor, darkening, 1F / HACK_BRIGHTNESS_STEP), lightThemeDesaturation)
			val perceivedBrightness = getPerceivedBrightness(lightThemeColor)
			if (perceivedBrightness <= lightThemeThreshold) {
				break
			}
		}
		
		for (brightening in 0..10) {
			darkThemeColor = ColorUtil.desaturate(ColorUtil.hackBrightness(baseColor, brightening, HACK_BRIGHTNESS_STEP), darkThemeDesaturation)
			val perceivedBrightness = getPerceivedBrightness(darkThemeColor)
			if (perceivedBrightness >= darkThemeThreshold) {
				break
			}
		}
		
		return lightThemeColor to darkThemeColor
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
