package com.chylex.intellij.inspectionlens.editor

import com.intellij.codeInsight.codeVision.ui.popup.layouter.bottom
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.codeInsight.hints.presentation.InputHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.paint.EffectPainter
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * Renders the text of an inspection lens.
 */
class LensRenderer(private var info: HighlightInfo) : HintRenderer(null), InputHandler {
	private lateinit var severity: LensSeverity
	private lateinit var inlay: Inlay<*>
	private var hovered = false
	
	init {
		setPropertiesFrom(info)
	}
	
	fun setInlay(inlay: Inlay<*>) {
		check(!this::inlay.isInitialized) { "Inlay already set" }
		this.inlay = inlay
	}
	
	fun setPropertiesFrom(info: HighlightInfo) {
		this.info = info
		
		text = getValidDescriptionText(info.description)
		severity = LensSeverity.from(info.severity)
	}
	
	override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
		fixBaselineForTextRendering(r)
		super.paint(inlay, g, r, textAttributes)
		
		if (hovered) {
			val editor = inlay.editor as EditorImpl
			val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
			
			g.color = severity.textAttributes.foregroundColor
			EffectPainter.LINE_UNDERSCORE.paint(g as Graphics2D, r.x + TEXT_PADDING_SIDE, r.y + editor.ascent + 1, r.width - UNDERLINE_SHRINKAGE, editor.descent, font)
		}
	}
	
	override fun getTextAttributes(editor: Editor): TextAttributes {
		return severity.textAttributes
	}
	
	override fun useEditorFont(): Boolean {
		return true
	}
	
	override fun mouseMoved(event: MouseEvent, translated: Point) {
		setHovered(true)
	}
	
	override fun mouseExited() {
		setHovered(false)
	}
	
	private fun setHovered(hovered: Boolean) {
		if (this.hovered == hovered) {
			return
		}
		
		this.hovered = hovered
		inlay.repaint()
		
		val cursor = Cursor.getPredefinedCursor(if (hovered) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR)
		val contentComponent = inlay.editor.contentComponent
		if (contentComponent.cursor != cursor) {
			UIUtil.setCursor(contentComponent, cursor)
		}
	}
	
	override fun mousePressed(event: MouseEvent, translated: Point) {
		if (!SwingUtilities.isLeftMouseButton(event)) {
			return
		}
		
		val bounds = inlay.bounds ?: return
		
		event.consume()
		IntentionPopup.show(info, inlay, RelativePoint(event.component, Point(bounds.x + TEXT_PADDING_SIDE + 1, bounds.bottom + 3)))
	}
	
	private companion object {
		/**
		 * [HintRenderer.paintHint] renders padding around text, but not around effects.
		 */
		private const val TEXT_PADDING_SIDE = 8
		private const val UNDERLINE_SHRINKAGE = (TEXT_PADDING_SIDE * 2) + 1
		
		private fun getValidDescriptionText(text: String?): String {
			return if (text.isNullOrBlank()) " " else addMissingPeriod(unescapeHtmlEntities(text))
		}
		
		private fun unescapeHtmlEntities(potentialHtml: String): String {
			return potentialHtml.ifContains('&', StringUtil::unescapeXmlEntities)
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
