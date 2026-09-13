package com.chylex.intellij.inspectionlens.editor.lens

import com.chylex.intellij.inspectionlens.InspectionLens
import com.chylex.intellij.inspectionlens.editor.Inspection
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfo.IntentionActionDescriptor
import com.intellij.codeInsight.daemon.impl.IntentionsUI
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass.IntentionsInfo
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.impl.CachedIntentions
import com.intellij.codeInsight.intention.impl.IntentionHintComponent
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import com.intellij.ui.awt.RelativePoint
import java.lang.reflect.Method

internal object IntentionsPopup {
	private const val INTENTION_SOURCE_CLASS_NAME = "com.intellij.codeInsight.intention.IntentionSource"
	
	private val showPopupMethod: ShowPopupMethod? = try {
		val method = IntentionHintComponent::class.java.declaredMethods.first { method ->
			val parameterTypes = method.parameterTypes
			
			method.name == "showPopup" &&
			parameterTypes.size == 2 &&
			parameterTypes[0] === RelativePoint::class.java &&
			parameterTypes[1].name == INTENTION_SOURCE_CLASS_NAME
		}
		
		method.isAccessible = true
		
		@Suppress("UNCHECKED_CAST")
		val intentionSourceEnum = Class.forName(INTENTION_SOURCE_CLASS_NAME) as Class<Enum<*>>
		val intentionSource = intentionSourceEnum.enumConstants.first { it.name == "OTHER" }
		
		ShowPopupMethod(method, arrayOf(null, intentionSource))
	} catch (t: Throwable) {
		InspectionLens.LOG.warn("Could not initialize intention popup", t)
		null
	}
	
	val hasShowPopupMethod
		get() = showPopupMethod != null
	
	private class ShowPopupMethod(private val method: Method, private val args: Array<Any?>) {
		operator fun invoke(component: IntentionHintComponent) {
			method.invoke(component, *args)
		}
	}
	
	fun show(inspection: Inspection, inlay: Inlay<*>) {
		if (!tryShow(inspection, inlay)) {
			HintManager.getInstance().showInformationHint(inlay.editor, "No context actions available at this location")
		}
	}
	
	private fun tryShow(inspection: Inspection, inlay: Inlay<*>): Boolean {
		val editor = inlay.editor
		val project = editor.project ?: return false
		val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return false
		
		PsiDocumentManager.getInstance(project).commitAllDocuments()
		IntentionsUI.getInstance(project).hide()
		
		val intentions = collectIntentions(editor, file, inspection.highlighter, inlay.offset)
		if (intentions == null) {
			return false
		}
		
		try {
			showIntentionsPopup(project, file, editor, intentions)
			return true
		} catch (t: Throwable) {
			InspectionLens.LOG.error("Could not show intention popup", t)
			return false
		}
	}
	
	private fun collectIntentions(editor: Editor, file: PsiFile, highlighter: RangeHighlighter, offset: Int): IntentionsInfo? {
		val resolvedInfo = HighlightInfo.fromRangeHighlighter(highlighter)
		if (resolvedInfo == null) {
			return null
		}
		
		val intentionActions = mutableListOf<IntentionActionDescriptor>()
		
		resolvedInfo.findRegisteredQuickFix { descriptor, _ ->
			if (ShowIntentionActionsHandler.availableFor(file, editor, offset, descriptor.action)) {
				intentionActions.add(descriptor)
			}
			null
		}
		
		return IntentionsInfo().also {
			it.offset = offset
			ShowIntentionsPass.fillIntentionsInfoForHighlightInfo(resolvedInfo, it, intentionActions)
		}
	}
	
	private fun showIntentionsPopup(project: Project, file: PsiFile, editor: Editor, intentions: IntentionsInfo) {
		if (intentions.isEmpty || showPopupMethod == null) {
			val showIntentionsAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS)
			val dataContext = DataManager.getInstance().getDataContext(editor.component)
			val event = AnActionEvent.createEvent(showIntentionsAction, dataContext, null, ActionPlaces.EDITOR_INLAY, ActionUiKind.NONE, null)
			ActionUtil.performAction(showIntentionsAction, event)
		}
		else {
			val cachedIntentions = CachedIntentions.create(project, file, editor, intentions)
			val hintComponent = IntentionHintComponent.showIntentionHint(project, file, editor, false, cachedIntentions)
			showPopupMethod.invoke(hintComponent)
		}
	}
	
}
