package com.chylex.intellij.inspectionlens.debug

import com.intellij.openapi.editor.Editor
import java.time.Instant

object LensEventManager {
	val fileNameToEventsMap = mutableMapOf<String, MutableList<LensEvent>>()
	
	@Synchronized
	fun addEvent(editor: Editor, event: LensEventData) {
		val path = editor.virtualFile?.path ?: return
		fileNameToEventsMap.getOrPut(path, ::mutableListOf).add(LensEvent(Instant.now(), event))
	}
}
