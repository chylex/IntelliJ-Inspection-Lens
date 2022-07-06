package com.chylex.intellij.inspectionlens

import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.vfs.VirtualFile

/**
 * Listens for newly opened editors, and attaches a [LensMarkupModelListener] to their document model.
 */
class LensFileEditorListener : FileEditorManagerListener {
	override fun fileOpenedSync(source: FileEditorManager, file: VirtualFile, editorsWithProviders: MutableList<FileEditorWithProvider>) {
		for (editorWrapper in editorsWithProviders) {
			val fileEditor = editorWrapper.fileEditor
			if (fileEditor is TextEditor) {
				val editor = fileEditor.editor
				val markupModel = DocumentMarkupModel.forDocument(editor.document, editor.project, false)
				if (markupModel is MarkupModelEx) {
					markupModel.addMarkupModelListener(fileEditor, LensMarkupModelListener(editor))
				}
			}
		}
	}
}
