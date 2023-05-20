package com.chylex.intellij.inspectionlens

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

/**
 * Installs [InspectionLens] in open editors when the plugin is loaded, and uninstalls it when the plugin is unloaded.
 */
class InspectionLensPluginListener : DynamicPluginListener {
	override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
		if (pluginDescriptor.pluginId.idString == InspectionLens.PLUGIN_ID) {
			InspectionLens.install()
		}
	}
	
	override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
		if (pluginDescriptor.pluginId.idString == InspectionLens.PLUGIN_ID) {
			InspectionLens.uninstall()
		}
	}
}
