package com.chylex.intellij.inspectionlens

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.ProjectManager

/**
 * Handles dynamic plugin loading.
 * 
 * On load, it installs the [LensMarkupModelListener] to all open editors.
 * On unload, it removes all lenses from all open editors.
 */
class InspectionLensPluginListener : DynamicPluginListener {
	companion object {
		private const val PLUGIN_ID = "com.chylex.intellij.inspectionlens"
		
		private inline fun ProjectManager.forEachEditor(action: (TextEditor) -> Unit) {
			for (project in this.openProjects.filterNot { it.isDisposed }) {
				val fileEditorManager = FileEditorManager.getInstance(project)
				
				for (editor in fileEditorManager.allEditors.filterIsInstance<TextEditor>()) {
					action(editor)
				}
			}
		}
	}
	
	override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
		if (pluginDescriptor.pluginId.idString == PLUGIN_ID) {
			ProjectManager.getInstanceIfCreated()?.forEachEditor(LensMarkupModelListener.Companion::install)
		}
	}
	
	override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
		if (pluginDescriptor.pluginId.idString == PLUGIN_ID) {
			ProjectManager.getInstanceIfCreated()?.forEachEditor {
				EditorInlayLensManager.remove(it.editor)
			}
		}
	}
}
