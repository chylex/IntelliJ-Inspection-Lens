package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.codeInsight.daemon.impl.IntentionsUI
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.lang.LangBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase

internal object IntentionsPopup {
	fun showAt(editor: Editor, offset: Int) {
		editor.caretModel.moveToOffset(offset)
		editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
		
		if (!tryShowPopup(editor)) {
			HintManager.getInstance().showInformationHint(editor, LangBundle.message("hint.text.no.context.actions.available.at.this.location"));
		}
	}
	
	private fun tryShowPopup(editor: Editor): Boolean {
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
