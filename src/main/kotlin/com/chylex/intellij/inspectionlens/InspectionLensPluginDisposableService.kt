package com.chylex.intellij.inspectionlens

import com.intellij.openapi.Disposable

/**
 * Gets automatically disposed when the plugin is unloaded.
 */
class InspectionLensPluginDisposableService : Disposable {
	override fun dispose() {}
}
