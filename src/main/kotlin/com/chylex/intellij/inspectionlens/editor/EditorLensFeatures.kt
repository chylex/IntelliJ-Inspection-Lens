package com.chylex.intellij.inspectionlens.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key

/**
 * Manages Inspection Lens features for a single [Editor].
 */
internal class EditorLensFeatures private constructor(
	editor: Editor,
	private val markupModel: MarkupModelEx,
	foldingModel: FoldingModelEx?,
	disposable: Disposable
) {
	private val lensManager = EditorLensManager(editor)
	private val lensManagerDispatcher = EditorLensManagerDispatcher(lensManager)
	private val markupModelListener = LensMarkupModelListener(lensManagerDispatcher)
	
	init {
		markupModel.addMarkupModelListener(disposable, markupModelListener)
		markupModelListener.showAllValid(markupModel.allHighlighters)
		
		foldingModel?.addListener(LensFoldingModelListener(lensManager), disposable)
	}
	
	private fun refresh() {
		markupModelListener.hideAll()
		markupModelListener.showAllValid(markupModel.allHighlighters)
	}
	
	companion object {
		private val EDITOR_KEY = Key<EditorLensFeatures>(EditorLensFeatures::class.java.name)
		
		fun install(editor: Editor, disposable: Disposable) {
			if (editor.getUserData(EDITOR_KEY) != null) {
				return
			}
			
			val markupModel = DocumentMarkupModel.forDocument(editor.document, editor.project, false) as? MarkupModelEx ?: return
			val foldingModel = editor.foldingModel as? FoldingModelEx
			val features = EditorLensFeatures(editor, markupModel, foldingModel, disposable)
			
			editor.putUserData(EDITOR_KEY, features)
			Disposer.register(disposable) { editor.putUserData(EDITOR_KEY, null) }
		}
		
		fun refresh(editor: Editor) {
			editor.getUserData(EDITOR_KEY)?.refresh()
		}
	}
}
