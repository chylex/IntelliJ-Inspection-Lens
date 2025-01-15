package com.chylex.intellij.inspectionlens.settings

import com.chylex.intellij.inspectionlens.InspectionLens
import com.chylex.intellij.inspectionlens.editor.lens.LensSeverityFilter
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.XMap

@State(
	name = LensApplicationConfigurable.ID,
	storages = [ Storage("chylex.inspectionLens.xml") ],
	category = SettingsCategory.UI
)
class LensSettingsState : SimplePersistentStateComponent<LensSettingsState.State>(State()) {
	class State : BaseState() {
		@get:XMap
		val hiddenSeverities by map<String, StoredSeverity>()
		
		var showUnknownSeverities by property(true)
		var useEditorFont by property(true)
		var maxDescriptionLength by property(120)
		var lensHoverMode by enum(LensHoverMode.DEFAULT)
	}
	
	companion object {
		val MAX_DESCRIPTION_LENGTH_RANGE = 20..1000
	}
	
	@get:Synchronized
	@set:Synchronized
	var severityFilter = createSeverityFilter()
		private set
	
	val useEditorFont
		get() = state.useEditorFont
	
	val maxDescriptionLength
		get() = state.maxDescriptionLength
	
	val lensHoverMode
		get() = state.lensHoverMode
	
	override fun loadState(state: State) {
		super.loadState(state)
		state.maxDescriptionLength = state.maxDescriptionLength.coerceIn(MAX_DESCRIPTION_LENGTH_RANGE)
		update()
	}
	
	fun resetState() {
		val default = State()
		
		state.hiddenSeverities.apply { clear(); putAll(default.hiddenSeverities) }
		state.showUnknownSeverities = default.showUnknownSeverities
		state.useEditorFont = default.useEditorFont
		state.maxDescriptionLength = default.maxDescriptionLength
		state.lensHoverMode = default.lensHoverMode
		
		update()
	}
	
	fun update() {
		severityFilter = createSeverityFilter()
		InspectionLens.scheduleRefresh()
	}
	
	private fun createSeverityFilter(): LensSeverityFilter {
		val state = state
		return LensSeverityFilter(state.hiddenSeverities.keys, state.showUnknownSeverities)
	}
}
