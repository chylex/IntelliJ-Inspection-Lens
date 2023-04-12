package com.chylex.intellij.inspectionlens

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileOpenedSyncListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.vfs.VirtualFile

/**
 * Listens for newly opened editors, and installs a [LensMarkupModelListener] on them.
 */
class LensFileOpenedListener : FileOpenedSyncListener {
	override fun fileOpenedSync(source: FileEditorManager, file: VirtualFile, editorsWithProviders: List<FileEditorWithProvider>) {
		for (editorWrapper in editorsWithProviders) {
			val fileEditor = editorWrapper.fileEditor
			if (fileEditor is TextEditor) {
				LensMarkupModelListener.install(fileEditor)
			}
		}
	}
}
