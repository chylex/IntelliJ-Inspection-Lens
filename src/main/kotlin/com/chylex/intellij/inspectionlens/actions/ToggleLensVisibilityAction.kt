package com.chylex.intellij.inspectionlens.actions

import com.chylex.intellij.inspectionlens.InspectionLens
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeepPopupOnPerform
import com.intellij.openapi.project.DumbAwareToggleAction

class ToggleLensVisibilityAction : DumbAwareToggleAction() {
	init {
		templatePresentation.keepPopupOnPerform = KeepPopupOnPerform.IfRequested
	}
	
	override fun getActionUpdateThread(): ActionUpdateThread {
		return ActionUpdateThread.BGT
	}
	
	override fun isSelected(e: AnActionEvent): Boolean {
		return InspectionLens.SHOW_LENSES
	}
	
	override fun setSelected(e: AnActionEvent, state: Boolean) {
		InspectionLens.SHOW_LENSES = state
	}
}
