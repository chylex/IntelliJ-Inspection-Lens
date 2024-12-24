package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor

internal class EditorLens private constructor(private var inlay: EditorLensInlay, private var lineBackground: EditorLensLineBackground) {
	fun update(info: HighlightInfo, settings: LensSettingsState): Boolean {
		val editor = inlay.editor
		
		if (!inlay.tryUpdate(info)) {
			inlay = EditorLensInlay.show(editor, info, settings) ?: return false
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
		fun show(editor: Editor, info: HighlightInfo, settings: LensSettingsState): EditorLens? {
			val inlay = EditorLensInlay.show(editor, info, settings) ?: return null
			val lineBackground = EditorLensLineBackground.show(editor, info)
			return EditorLens(inlay, lineBackground)
		}
	}
}
