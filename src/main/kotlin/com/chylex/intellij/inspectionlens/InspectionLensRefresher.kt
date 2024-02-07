package com.chylex.intellij.inspectionlens

import com.intellij.openapi.application.ApplicationManager

object InspectionLensRefresher {
	private var needsRefresh = false
	
	fun scheduleRefresh() {
		synchronized(this) {
			if (!needsRefresh) {
				needsRefresh = true
				ApplicationManager.getApplication().invokeLater(::refresh)
			}
		}
	}
	
	private fun refresh() {
		synchronized(this) {
			if (needsRefresh) {
				needsRefresh = false
				InspectionLens.refresh()
			}
		}
	}
}
