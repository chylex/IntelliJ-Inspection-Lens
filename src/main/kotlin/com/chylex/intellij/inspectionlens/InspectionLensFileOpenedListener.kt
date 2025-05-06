package com.chylex.intellij.inspectionlens

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileOpenedSyncListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.vfs.VirtualFile

/**
 * Installs [InspectionLens] in newly opened editors.
 */
class InspectionLensFileOpenedListener : FileOpenedSyncListener {
	override fun fileOpenedSync(source: FileEditorManager, file: VirtualFile, editorsWithProviders: List<FileEditorWithProvider>) {
		InspectionLens.LOG.info("File opened: $file (editor count: ${editorsWithProviders.size})")
		
		for (editorWrapper in editorsWithProviders) {
			val fileEditor = editorWrapper.fileEditor
			if (fileEditor is TextEditor) {
				InspectionLens.install(fileEditor)
			}
		}
	}
}
