package com.chylex.intellij.inspectionlens

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileOpenedSyncListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.VirtualFile

/**
 * Installs [InspectionLens] in opened editors.
 *
 * Used instead of [FileOpenedSyncListener], because [FileOpenedSyncListener] randomly fails to register during IDE startup.
 */
class InspectionLensFileEditorManagerListener : FileEditorManagerListener {
	override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
		for (editor in source.getEditors(file)) {
			if (editor is TextEditor) {
				InspectionLens.install(editor)
			}
		}
	}
}
