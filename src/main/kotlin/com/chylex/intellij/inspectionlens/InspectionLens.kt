package com.chylex.intellij.inspectionlens

import com.chylex.intellij.inspectionlens.editor.EditorLensFeatures
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.ProjectManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles installation and uninstallation of plugin features in editors.
 */
internal object InspectionLens {
	const val PLUGIN_ID = "com.chylex.intellij.inspectionlens"
	
	val LOG = logger<InspectionLens>()
	
	var SHOW_LENSES = true
		set(value) {
			field = value
			scheduleRefresh()
		}
	
	/**
	 * Installs lenses into [editor].
	 */
	fun install(editor: TextEditor) {
		EditorLensFeatures.install(editor)
	}
	
	/**
	 * Installs lenses into all open editors.
	 */
	fun install() {
		forEachOpenEditor(EditorLensFeatures::install)
	}
	
	/**
	 * Refreshes lenses in all open editors.
	 */
	private fun refresh() {
		forEachOpenEditor(EditorLensFeatures::refresh)
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
		
		for (project in projectManager.openProjects) {
			if (project.isDisposed) {
				continue
			}
			
			for (editor in FileEditorManager.getInstance(project).allEditors) {
				if (editor is TextEditor) {
					action(editor)
				}
			}
		}
	}
	
	private object Refresh : Runnable {
		private val needsRefresh = AtomicBoolean(false)
		
		fun schedule() {
			if (needsRefresh.compareAndSet(false, true)) {
				ApplicationManager.getApplication().invokeLater(this)
			}
		}
		
		override fun run() {
			if (needsRefresh.compareAndSet(true, false)) {
				refresh()
			}
		}
	}
}
