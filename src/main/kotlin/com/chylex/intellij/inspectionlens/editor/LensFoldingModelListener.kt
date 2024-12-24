package com.chylex.intellij.inspectionlens.editor

import com.intellij.openapi.editor.ex.FoldingListener

/**
 * Listens for code folding events and reports them to [EditorLensManager].
 */
internal class LensFoldingModelListener(private val lensManager: EditorLensManager) : FoldingListener {
	override fun onFoldProcessingEnd() {
		lensManager.onFoldRegionsChanged()
	}
}
