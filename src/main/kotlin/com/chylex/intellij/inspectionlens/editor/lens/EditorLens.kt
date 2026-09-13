package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.editor.Inspection
import com.chylex.intellij.inspectionlens.settings.LensSettingsState
import com.intellij.openapi.editor.Editor

internal class EditorLens private constructor(private var inlay: EditorLensInlay, private var lineBackground: EditorLensLineBackground, private var severity: LensSeverity) {
	fun update(inspection: Inspection, settings: LensSettingsState): Boolean {
		val editor = inlay.editor
		val oldSeverity = severity
		
		severity = LensSeverity.from(inspection.info.severity)
		
		if (!inlay.tryUpdate(inspection)) {
			inlay = EditorLensInlay.show(editor, inspection, settings) ?: return false
		}
		
		if (lineBackground.isInvalid || oldSeverity != severity) {
			lineBackground.hide(editor)
			lineBackground = EditorLensLineBackground.show(editor, inspection.info)
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
		fun show(editor: Editor, inspection: Inspection, settings: LensSettingsState): EditorLens? {
			val info = inspection.info
			val inlay = EditorLensInlay.show(editor, inspection, settings) ?: return null
			val lineBackground = EditorLensLineBackground.show(editor, info)
			val severity = LensSeverity.from(info.severity)
			return EditorLens(inlay, lineBackground, severity)
		}
	}
}
