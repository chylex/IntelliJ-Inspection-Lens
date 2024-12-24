package com.chylex.intellij.inspectionlens

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.rd.createNestedDisposable
import com.jetbrains.rd.util.lifetime.Lifetime

/**
 * Gets automatically disposed when the plugin is unloaded.
 */
@Service
class InspectionLensPluginDisposableService : Disposable {
	/**
	 * Creates a [Disposable] that will be disposed when either plugin is unloaded, or the [other] [Disposable] is disposed.
	 */
	fun intersect(other: Disposable): Disposable {
		return Lifetime.intersect(createLifetime(), other.createLifetime()).createNestedDisposable("InspectionLensIntersectedLifetime")
	}
	
	override fun dispose() {}
}
