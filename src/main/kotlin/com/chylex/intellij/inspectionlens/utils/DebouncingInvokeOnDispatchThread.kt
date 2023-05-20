package com.chylex.intellij.inspectionlens.utils

import com.intellij.openapi.application.ApplicationManager

class DebouncingInvokeOnDispatchThread<T>(private val action: (List<T>) -> Unit) {
	private var queuedItems = mutableListOf<T>()
	private var isEnqueued = false
	
	fun enqueue(item: T) {
		synchronized(this) {
			queuedItems.add(item)
			
			// Enqueue even if already on dispatch thread to debounce consecutive calls.
			if (!isEnqueued) {
				isEnqueued = true
				ApplicationManager.getApplication().invokeLater(::process)
			}
		}
	}
	
	private fun process() {
		var itemsToProcess: List<T>
		
		synchronized(this) {
			itemsToProcess = queuedItems
			queuedItems = mutableListOf()
			isEnqueued = false
		}
		
		action(itemsToProcess)
	}
}
