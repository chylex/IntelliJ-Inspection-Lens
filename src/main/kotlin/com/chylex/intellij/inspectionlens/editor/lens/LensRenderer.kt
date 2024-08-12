package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.codeInsight.hints.presentation.InputHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.paint.EffectPainter
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
class LensRenderer(private var info: HighlightInfo, settings: LensSettingsState) : HintRenderer(null), InputHandler {
	private val useEditorFont = settings.useEditorFont
	private lateinit var inlay: Inlay<*>
	private lateinit var attributes: LensSeverityTextAttributes
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
		attributes = LensSeverity.from(info.severity).textAttributes
	}
	
	override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
		fixBaselineForTextRendering(r)
		super.paint(inlay, g, r, textAttributes)
		
		if (hovered) {
			paintHoverEffect(inlay, g, r)
		}
	}
	
	private fun paintHoverEffect(inlay: Inlay<*>, g: Graphics, r: Rectangle) {
		val editor = inlay.editor
		if (editor !is EditorImpl) {
			return
		}
		
		val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
		val x = r.x + TEXT_HORIZONTAL_PADDING
		val y = r.y + editor.ascent + 1
		val w = inlay.widthInPixels - UNDERLINE_WIDTH_REDUCTION
		val h = editor.descent
		
		g.color = attributes.foregroundColor
		EffectPainter.LINE_UNDERSCORE.paint(g as Graphics2D, x, y, w, h, font)
	}
	
	override fun getTextAttributes(editor: Editor): TextAttributes {
		return attributes
	}
	
	override fun useEditorFont(): Boolean {
		return useEditorFont
	}
	
	override fun mouseMoved(event: MouseEvent, translated: Point) {
		setHovered(isHoveringText(translated))
	}
	
	override fun mouseExited() {
		setHovered(false)
	}
	
	private fun setHovered(hovered: Boolean) {
		if (this.hovered == hovered) {
			return
		}
		
		this.hovered = hovered
		
		val editor = inlay.editor
		if (editor is EditorEx) {
			val cursor = if (hovered) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else null
			editor.setCustomCursor(this::class.java, cursor)
		}
		
		inlay.repaint()
	}
	
	override fun mousePressed(event: MouseEvent, translated: Point) {
		if (!SwingUtilities.isLeftMouseButton(event) || !isHoveringText(translated)) {
			return
		}
		
		event.consume()
		IntentionsPopup.showAt(inlay.editor, info.actualStartOffset)
	}
	
	private fun isHoveringText(point: Point): Boolean {
		return point.x >= HOVER_PADDING_LEFT
			&& point.y >= 4
			&& point.x < inlay.widthInPixels - HOVER_PADDING_RIGHT
			&& point.y < inlay.heightInPixels - 1
	}
	
	private companion object {
		/**
		 * [HintRenderer.paintHint] renders padding around text, but not around effects.
		 */
		private const val TEXT_HORIZONTAL_PADDING = 7
		
		/**
		 * The last character is always a period, which does not take up the full width, so the underline and the hover region are shrunk by an additional pixel.
		 */
		private const val EXTRA_RIGHT_SIDE_PADDING = 1
		
		private const val UNDERLINE_WIDTH_REDUCTION = (TEXT_HORIZONTAL_PADDING * 2) + EXTRA_RIGHT_SIDE_PADDING
		private const val HOVER_PADDING_LEFT = TEXT_HORIZONTAL_PADDING - 2
		private const val HOVER_PADDING_RIGHT = HOVER_PADDING_LEFT + EXTRA_RIGHT_SIDE_PADDING
		
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
