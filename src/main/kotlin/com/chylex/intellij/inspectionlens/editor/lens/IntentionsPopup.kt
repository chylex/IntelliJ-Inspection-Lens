package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.InspectionLens
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfo.IntentionActionDescriptor
import com.intellij.codeInsight.daemon.impl.IntentionsUI
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass.IntentionsInfo
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.impl.CachedIntentions
import com.intellij.codeInsight.intention.impl.IntentionHintComponent
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.lang.LangBundle
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.concurrency.AppExecutorUtil
import java.lang.reflect.Method

internal object IntentionsPopup {
	private const val INTENTION_SOURCE_CLASS_NAME = "com.intellij.codeInsight.intention.IntentionSource"
	
	private val showPopupMethod: ShowPopupMethod? = try {
		val method = IntentionHintComponent::class.java.declaredMethods.first { method ->
			val parameterTypes = method.parameterTypes
			
			method.name == "showPopup" &&
			parameterTypes.size in 1..2 &&
			parameterTypes[0] === RelativePoint::class.java &&
			parameterTypes.getOrNull(1).let { p -> p == null || p.name == INTENTION_SOURCE_CLASS_NAME }
		}
		
		method.isAccessible = true
		
		@Suppress("UNCHECKED_CAST")
		val args: Array<Any?> = if (method.parameterCount == 1)
			arrayOf(null)
		else
			arrayOf(null, (Class.forName(INTENTION_SOURCE_CLASS_NAME) as Class<Enum<*>>).enumConstants.first { it.name == "OTHER" })
		
		ShowPopupMethod(method, args)
	} catch (t: Throwable) {
		InspectionLens.LOG.warn("Could not initialize intention popup", t)
		null
	}
	
	private class ShowPopupMethod(private val method: Method, private val args: Array<Any?>) {
		operator fun invoke(component: IntentionHintComponent) {
			method.invoke(component, *args)
		}
	}
	
	fun show(highlightInfo: HighlightInfo, inlay: Inlay<*>) {
		if (!tryShow(highlightInfo, inlay)) {
			showNoActionsAvailable(inlay.editor)
		}
	}
	
	private fun tryShow(highlightInfo: HighlightInfo, inlay: Inlay<*>): Boolean {
		val editor = inlay.editor
		val project = editor.project ?: return false
		val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return false
		
		PsiDocumentManager.getInstance(project).commitAllDocuments()
		IntentionsUI.getInstance(project).hide()
		
		ReadAction
			.nonBlocking<IntentionsInfo> { collectIntentions(editor, project, file, highlightInfo, inlay.offset) }
			.finishOnUiThread(ModalityState.current()) { tryShowPopup(project, file, editor, it) }
			.submit(AppExecutorUtil.getAppExecutorService())
		
		return true
	}
	
	private fun collectIntentions(editor: Editor, project: Project, file: PsiFile, info: HighlightInfo, offset: Int): IntentionsInfo {
		val intentions = mutableListOf<IntentionActionDescriptor>()
		
		info.findRegisteredQuickFix { descriptor, _ ->
			if (DumbService.getInstance(project).isUsableInCurrentContext(descriptor.action) && ShowIntentionActionsHandler.availableFor(file, editor, offset, descriptor.action)) {
				intentions.add(descriptor)
			}
			null
		}
		
		return IntentionsInfo().also {
			it.offset = offset
			
			if (info.severity === HighlightSeverity.ERROR) {
				it.errorFixesToShow.addAll(intentions)
			}
			else {
				it.inspectionFixesToShow.addAll(intentions)
			}
		}
	}
	
	private fun tryShowPopup(project: Project, file: PsiFile, editor: Editor, intentions: IntentionsInfo) {
		try {
			showPopup(project, file, editor, intentions)
		} catch (t: Throwable) {
			InspectionLens.LOG.error("Could not show intention popup", t)
			showNoActionsAvailable(editor)
		}
	}
	
	private fun showPopup(project: Project, file: PsiFile, editor: Editor, intentions: IntentionsInfo) {
		if (intentions.isEmpty || showPopupMethod == null) {
			val showIntentionsAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS)
			ActionUtil.invokeAction(showIntentionsAction, editor.component, ActionPlaces.EDITOR_INLAY, null, null)
		}
		else {
			val cachedIntentions = CachedIntentions.create(project, file, editor, intentions)
			val hintComponent = IntentionHintComponent.showIntentionHint(project, file, editor, false, cachedIntentions)
			showPopupMethod.invoke(hintComponent)
		}
	}
	
	private fun showNoActionsAvailable(editor: Editor) {
		HintManager.getInstance().showInformationHint(editor, LangBundle.message("hint.text.no.context.actions.available.at.this.location"))
	}
}
