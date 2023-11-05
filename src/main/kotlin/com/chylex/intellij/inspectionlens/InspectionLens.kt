package com.chylex.intellij.inspectionlens

import com.chylex.intellij.inspectionlens.editor.EditorLensManager
import com.chylex.intellij.inspectionlens.editor.LensMarkupModelListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.rd.createNestedDisposable
import com.jetbrains.rd.util.lifetime.Lifetime

/**
 * Handles installation and uninstallation of plugin features in editors.
 */
internal object InspectionLens {
	const val PLUGIN_ID = "com.chylex.intellij.inspectionlens"
	
	/**
	 * Installs lenses into [editor].
	 */
	fun install(editor: TextEditor) {
		LensMarkupModelListener.register(editor.editor, createEditorDisposable(editor))
	}
	
	/**
	 * Installs lenses into all open editors.
	 */
	fun install() {
		forEachOpenEditor(::install)
	}
	
	/**
	 * Uninstalls lenses from all open editors.
	 */
	fun uninstall() {
		forEachOpenEditor {
			EditorLensManager.remove(it.editor)
		}
	}
	
	/**
	 * Refreshes lenses in all open editors.
	 */
	fun refresh() {
		forEachOpenEditor {
			LensMarkupModelListener.refresh(it.editor)
		}
	}
	
	/**
	 * Creates a [Disposable] that will be disposed when either the [TextEditor] is disposed or the plugin is unloaded.
	 */
	private fun createEditorDisposable(textEditor: TextEditor): Disposable {
		val pluginLifetime = ApplicationManager.getApplication().getService(InspectionLensPluginDisposableService::class.java).createLifetime()
		val editorLifetime = textEditor.createLifetime()
		return Lifetime.intersect(pluginLifetime, editorLifetime).createNestedDisposable("InspectionLensIntersectedLifetime")
	}
	
	/**
	 * Executes [action] on all open editors.
	 */
	private inline fun forEachOpenEditor(action: (TextEditor) -> Unit) {
		val projectManager = ProjectManager.getInstanceIfCreated() ?: return
		
		for (project in projectManager.openProjects.filterNot { it.isDisposed }) {
			for (editor in FileEditorManager.getInstance(project).allEditors.filterIsInstance<TextEditor>()) {
				action(editor)
			}
		}
	}
}
