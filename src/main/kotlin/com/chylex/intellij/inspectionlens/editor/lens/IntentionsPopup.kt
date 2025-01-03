package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.codeInsight.daemon.impl.IntentionsUI
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.actions.ShowIntentionActionsAction
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.lang.LangBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase

internal object IntentionsPopup {
	fun show(editor: Editor) {
		if (!tryShow(editor)) {
			HintManager.getInstance().showInformationHint(editor, LangBundle.message("hint.text.no.context.actions.available.at.this.location"))
		}
	}
	
	private fun tryShow(editor: Editor): Boolean {
		// If the IDE uses the default Show Intentions action and handler,
		// use the handler directly to bypass additional logic from the action.
		val action = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS)
		if (action.javaClass === ShowIntentionActionsAction::class.java) {
			return tryShowWithDefaultHandler(editor)
		}
		else {
			ActionUtil.invokeAction(action, editor.component, ActionPlaces.EDITOR_INLAY, null, null)
			return true
		}
	}
	
	private fun tryShowWithDefaultHandler(editor: Editor): Boolean {
		val project = editor.project ?: return false
		val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return false
		
		PsiDocumentManager.getInstance(project).commitAllDocuments()
		IntentionsUI.getInstance(project).hide()
		
		HANDLER.showIntentionHint(project, editor, file, showFeedbackOnEmptyMenu = true)
		return true
	}
	
	private val HANDLER = object : ShowIntentionActionsHandler() {
		public override fun showIntentionHint(project: Project, editor: Editor, file: PsiFile, showFeedbackOnEmptyMenu: Boolean) {
			super.showIntentionHint(project, editor, file, showFeedbackOnEmptyMenu)
		}
	}
}
