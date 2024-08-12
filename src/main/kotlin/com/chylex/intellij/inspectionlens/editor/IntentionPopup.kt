package com.chylex.intellij.inspectionlens.editor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfo.IntentionActionDescriptor
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass.IntentionsInfo
import com.intellij.codeInsight.intention.impl.CachedIntentions
import com.intellij.codeInsight.intention.impl.IntentionHintComponent
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.concurrency.AppExecutorUtil
import java.lang.invoke.MethodHandles

internal object IntentionPopup {
	fun show(info: HighlightInfo, inlay: Inlay<*>, point: RelativePoint) {
		val editor = inlay.editor
		val project = editor.project ?: return
		val psiFile = editor.virtualFile.findPsiFile(project) ?: return
		
		ReadAction
			.nonBlocking<IntentionsInfo> { collectIntentions(editor, project, psiFile, info, inlay.offset) }
			.finishOnUiThread(ModalityState.current()) { showPopup(project, psiFile, editor, it, point) }
			.submit(AppExecutorUtil.getAppExecutorService())
	}
	
	private fun collectIntentions(editor: Editor, project: Project, psiFile: PsiFile, info: HighlightInfo, offset: Int): IntentionsInfo {
		val intentions = mutableListOf<IntentionActionDescriptor>()
		
		info.findRegisteredQuickFix { descriptor, _ ->
			if (DumbService.getInstance(project).isUsableInCurrentContext(descriptor.action) && ShowIntentionActionsHandler.availableFor(psiFile, editor, offset, descriptor.action)) {
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
	
	private val showPopupMethod by lazy {
		IntentionHintComponent::class.java.getDeclaredMethod("showPopup", RelativePoint::class.java)
			.also { it.isAccessible = true }
			.let(MethodHandles.lookup()::unreflect)
	}
	
	private fun showPopup(project: Project, psiFile: PsiFile, editor: Editor, intentions: IntentionsInfo, point: RelativePoint) {
		val cachedIntentions = CachedIntentions.create(project, psiFile, editor, intentions)
		val component = IntentionHintComponent.showIntentionHint(project, psiFile, editor, false, cachedIntentions)
		showPopupMethod.invoke(component, point)
	}
}
