package com.chylex.intellij.inspectionlens.editor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor

internal class EditorLens private constructor(private var inlay: EditorLensInlay, private var lineBackground: EditorLensLineBackground) {
	fun update(info: HighlightInfo): Boolean {
		val editor = inlay.editor
		
		if (!inlay.tryUpdate(info)) {
			inlay = EditorLensInlay.show(editor, info) ?: return false
		}
		
		if (lineBackground.shouldRecreate(info)) {
			lineBackground.hide(editor)
			lineBackground = EditorLensLineBackground.show(editor, info)
		}
		
		return true
	}
	
	fun hide() {
		val editor = inlay.editor
		
		inlay.hide()
		lineBackground.hide(editor)
	}
	
	companion object {
		fun show(editor: Editor, info: HighlightInfo): EditorLens? {
			val inlay = EditorLensInlay.show(editor, info) ?: return null
			val lineBackground = EditorLensLineBackground.show(editor, info)
			return EditorLens(inlay, lineBackground)
		}
	}
}
