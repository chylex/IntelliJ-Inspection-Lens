package com.chylex.intellij.inspectionlens.editor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor

internal class EditorLens private constructor(private var inlay: EditorLensInlay) {
	fun update(info: HighlightInfo): Boolean {
		val editor = inlay.editor
		
		if (!inlay.tryUpdate(info)) {
			inlay = EditorLensInlay.show(editor, info) ?: return false
		}
		
		return true
	}
	
	fun hide() {
		inlay.hide()
	}
	
	companion object {
		fun show(editor: Editor, info: HighlightInfo): EditorLens? {
			return EditorLensInlay.show(editor, info)?.let(::EditorLens)
		}
	}
}
