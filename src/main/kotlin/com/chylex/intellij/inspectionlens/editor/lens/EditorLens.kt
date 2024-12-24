package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor

internal class EditorLens private constructor(private var inlay: EditorLensInlay, private var lineBackground: EditorLensLineBackground, private var severity: LensSeverity) {
	fun update(info: HighlightInfo, settings: LensSettingsState): Boolean {
		val editor = inlay.editor
		val oldSeverity = severity
		
		severity = LensSeverity.from(info.severity)
		
		if (!inlay.tryUpdate(info)) {
			inlay = EditorLensInlay.show(editor, info, settings) ?: return false
		}
		
		if (lineBackground.isInvalid || oldSeverity != severity) {
			lineBackground.hide(editor)
			lineBackground = EditorLensLineBackground.show(editor, info)
		}
		
		return true
	}
	
	fun onFoldRegionsChanged() {
		lineBackground.onFoldRegionsChanged(inlay.editor, severity)
	}
	
	fun hide() {
		inlay.hide()
		lineBackground.hide(inlay.editor)
	}
	
	companion object {
		fun show(editor: Editor, info: HighlightInfo, settings: LensSettingsState): EditorLens? {
			val inlay = EditorLensInlay.show(editor, info, settings) ?: return null
			val lineBackground = EditorLensLineBackground.show(editor, info)
			val severity = LensSeverity.from(info.severity)
			return EditorLens(inlay, lineBackground, severity)
		}
	}
}
