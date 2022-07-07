package com.chylex.intellij.inspectionlens.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.lang.ref.WeakReference

/**
 * A [Disposable] that can have multiple parents, and will be disposed when any parent is disposed.
 * A [WeakReference] and a lambda will remain in memory for every parent that is not disposed.
 */
class MultiParentDisposable {
	val self = Disposer.newDisposable()
	
	fun registerWithParent(parent: Disposable) {
		val weakSelfReference = WeakReference(self)
		Disposer.register(parent) { weakSelfReference.get()?.let(Disposer::dispose) }
	}
}
