package com.chylex.intellij.inspectionlens

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

/**
 * Installs [InspectionLens] in open editors when the plugin is loaded.
 */
class InspectionLensPluginListener : DynamicPluginListener {
	override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
		if (pluginDescriptor.pluginId.idString == InspectionLens.PLUGIN_ID) {
			InspectionLens.install()
		}
	}
}
