package com.chylex.intellij.inspectionlens

import com.chylex.intellij.inspectionlens.editor.EditorLensFeatures
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.ProjectManager

/**
 * Handles installation and uninstallation of plugin features in editors.
 */
internal object InspectionLens {
	const val PLUGIN_ID = "com.chylex.intellij.inspectionlens"
	
	val LOG = logger<InspectionLens>()
	
	/**
	 * Installs lenses into [editor].
	 */
	fun install(editor: TextEditor) {
		EditorLensFeatures.install(editor.editor, service<InspectionLensPluginDisposableService>().intersect(editor))
	}
	
	/**
	 * Installs lenses into all open editors.
	 */
	fun install() {
		forEachOpenEditor(::install)
	}
	
	/**
	 * Refreshes lenses in all open editors.
	 */
	private fun refresh() {
		forEachOpenEditor {
			EditorLensFeatures.refresh(it.editor)
		}
	}
	
	/**
	 * Schedules a refresh of lenses in all open editors.
	 */
	fun scheduleRefresh() {
		Refresh.schedule()
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
	
	private object Refresh {
		private var needsRefresh = false
		
		fun schedule() {
			synchronized(this) {
				if (!needsRefresh) {
					needsRefresh = true
					ApplicationManager.getApplication().invokeLater(this::run)
				}
			}
		}
		
		private fun run() {
			synchronized(this) {
				if (needsRefresh) {
					needsRefresh = false
					refresh()
				}
			}
		}
	}
}
