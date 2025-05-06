package com.chylex.intellij.inspectionlens.debug

sealed interface LensEventData {
	data class MarkupModelAfterAdded(val lens: Highlighter) : LensEventData
	data class MarkupModelAttributesChanged(val lens: Highlighter) : LensEventData
	data class MarkupModelBeforeRemoved(val lens: Highlighter) : LensEventData
}
