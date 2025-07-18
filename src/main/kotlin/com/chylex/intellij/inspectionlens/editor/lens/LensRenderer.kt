package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.settings.LensHoverMode
import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.codeInsight.hints.presentation.InputHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.paint.EffectPainter
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.util.regex.Pattern
import javax.swing.SwingUtilities

/**
 * Renders the text of an inspection lens.
 */
class LensRenderer(private var info: HighlightInfo, private val settings: LensSettingsState) : HintRenderer(null), InputHandler {
	private lateinit var inlay: Inlay<*>
	private lateinit var attributes: LensSeverityTextAttributes
	private var extraRightPadding = 0
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
		val description = getValidDescriptionText(info.description, settings.maxDescriptionLength)
		
		text = description
		attributes = LensSeverity.from(info.severity).textAttributes
		extraRightPadding = if (description.lastOrNull() == '.') 2 else 0
	}
	
	override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
		fixBaselineForTextRendering(r)
		super.paint(inlay, g, r, textAttributes)
		
		if (hovered && isHoveringText()) {
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
		val y = r.y + editor.ascent
		val w = inlay.widthInPixels - UNDERLINE_WIDTH_REDUCTION - extraRightPadding
		val h = editor.descent
		
		g.color = attributes.foregroundColor
		EffectPainter.LINE_UNDERSCORE.paint(g as Graphics2D, x, y, w, h, font)
	}
	
	override fun getTextAttributes(editor: Editor): TextAttributes {
		return attributes
	}
	
	override fun useEditorFont(): Boolean {
		return settings.useEditorFont
	}
	
	override fun mouseMoved(event: MouseEvent, translated: Point) {
		setHovered(isHoveringText(translated))
	}
	
	override fun mouseExited() {
		setHovered(false)
	}
	
	private fun setHovered(hovered: Boolean) {
		if (hovered && settings.lensHoverMode == LensHoverMode.DISABLED) {
			return
		}
		
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
		val hoverMode = settings.lensHoverMode
		if (hoverMode == LensHoverMode.DISABLED || !isHoveringText(translated)) {
			return
		}
		
		if (event.button.let { it == MouseEvent.BUTTON1 || it == MouseEvent.BUTTON2 }) {
			event.consume()
			
			val editor = inlay.editor
			moveToOffset(editor, info.actualStartOffset)
			
			if ((event.button == MouseEvent.BUTTON1) xor (hoverMode != LensHoverMode.DEFAULT)) {
				IntentionsPopup.show(info, inlay)
			}
		}
	}
	
	private fun isHoveringText(): Boolean {
		val bounds = inlay.bounds ?: return false
		val translatedPoint = MouseInfo.getPointerInfo().location.apply {
			SwingUtilities.convertPointFromScreen(this, inlay.editor.contentComponent)
			translate(-bounds.x, -bounds.y)
		}
		
		return isHoveringText(translatedPoint)
	}
	
	private fun isHoveringText(point: Point): Boolean {
		return point.x >= HOVER_HORIZONTAL_PADDING
			&& point.y >= 4
			&& point.x < inlay.widthInPixels - HOVER_HORIZONTAL_PADDING - extraRightPadding
			&& point.y < inlay.heightInPixels - 1
	}
	
	private companion object {
		/**
		 * [HintRenderer.paintHint] renders padding around text, but not around effects.
		 */
		private const val TEXT_HORIZONTAL_PADDING = 7
		private const val HOVER_HORIZONTAL_PADDING = TEXT_HORIZONTAL_PADDING - 2
		private const val UNDERLINE_WIDTH_REDUCTION = (TEXT_HORIZONTAL_PADDING * 2) - 1
		
		/**
		 * Kotlin compiler inspections have an `[UPPERCASE_TAG]` at the beginning.
		 */
		private val UPPERCASE_TAG_REGEX = Pattern.compile("^\\[[A-Z_]+] ")
		
		private fun getValidDescriptionText(text: String?, maxLength: Int): String {
			return if (text.isNullOrBlank()) " " else addEllipsisOrMissingPeriod(unescapeHtmlEntities(stripUppercaseTag(text)), maxLength)
		}
		
		private fun stripUppercaseTag(text: String): String {
			if (text.startsWith('[')) {
				val matcher = UPPERCASE_TAG_REGEX.matcher(text)
				if (matcher.find()) {
					return text.substring(matcher.end())
				}
			}
			
			return text
		}
		
		private fun unescapeHtmlEntities(text: String): String {
			return if (text.contains('&')) StringUtil.unescapeXmlEntities(text) else text
		}
		
		private fun addEllipsisOrMissingPeriod(text: String, maxLength: Int): String {
			return when {
				text.length > maxLength -> text.take(maxLength).trimEnd { it.isWhitespace() || it == '.' } + "…"
				!text.endsWith('.')     -> "$text."
				else                    -> text
			}
		}
		
		private fun fixBaselineForTextRendering(rect: Rectangle) {
			rect.y += 1
		}
		
		private fun moveToOffset(editor: Editor, offset: Int) {
			editor.caretModel.moveToOffset(offset)
			editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
		}
	}
}
