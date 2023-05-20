package com.chylex.intellij.inspectionlens.editor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.text.StringUtil
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Renders the text of an inspection lens.
 */
class LensRenderer(info: HighlightInfo) : HintRenderer(null) {
	private lateinit var severity: LensSeverity
	
	init {
		setPropertiesFrom(info)
	}
	
	fun setPropertiesFrom(info: HighlightInfo) {
		text = getValidDescriptionText(info.description)
		severity = LensSeverity.from(info.severity)
	}
	
	override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
		fixBaselineForTextRendering(r)
		super.paint(inlay, g, r, textAttributes)
	}
	
	override fun getTextAttributes(editor: Editor): TextAttributes {
		return severity.colorAttributes
	}
	
	override fun useEditorFont(): Boolean {
		return true
	}
	
	private companion object {
		private fun getValidDescriptionText(text: String?): String {
			return if (text.isNullOrBlank()) " " else addMissingPeriod(convertHtmlToText(text))
		}
		
		private fun convertHtmlToText(potentialHtml: String): String {
			return potentialHtml
				.ifContains('<') { StringUtil.stripHtml(it, " ") }
				.ifContains('&', StringUtil::unescapeXmlEntities)
		}
		
		private fun addMissingPeriod(text: String): String {
			return if (text.endsWith('.')) text else "$text."
		}
		
		private inline fun String.ifContains(charToTest: Char, action: (String) -> String): String {
			return if (this.contains(charToTest)) action(this) else this
		}
		
		private fun fixBaselineForTextRendering(rect: Rectangle) {
			rect.y += 1
		}
	}
}
