package com.chylex.intellij.inspectionlens.debug

import com.chylex.intellij.inspectionlens.editor.lens.ColorGenerator
import com.chylex.intellij.inspectionlens.editor.lens.ColorMode
import com.chylex.intellij.inspectionlens.editor.lens.EditorLens
import com.chylex.intellij.inspectionlens.editor.lens.LensSeverity
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.grazie.ide.TextProblemSeverities
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.spellchecker.SpellCheckerSeveritiesProvider
import com.intellij.ui.ColorUtil
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import javax.swing.Action
import javax.swing.JSlider

class ShowColorEditorAction : AnAction() {
	override fun getActionUpdateThread(): ActionUpdateThread {
		return ActionUpdateThread.BGT
	}
	
	override fun actionPerformed(e: AnActionEvent) {
		Dialog(e.project).show()
	}
	
	private class Dialog(project: Project?) : DialogWrapper(project) {
		private val editorSyncer = EditorSyncer(disposable)
		
		init {
			title = "Inspection Lens - Color Editor"
			init()
		}
		
		@Suppress("DuplicatedCode")
		override fun createCenterPanel(): DialogPanel {
			return panel {
				row {
					for (scheme in EditorColorsManager.getInstance().allSchemes.sortedWith(compareBy({ ColorUtil.isDark(it.defaultBackground) }, { it.displayName }))) {
						createEditor(scheme)
					}
				}.resizableRow()
				
				row {
					val generateColors = checkBox("Generate colors")
						.selected(ColorGenerator.useGenerator)
						.onChanged {
							ColorGenerator.useGenerator = it.isSelected
							refreshColors()
						}
					
					slider(0, 15, 1, 5)
						.label("Light theme desaturation", LabelPosition.TOP)
						.enabledIf(generateColors.selected)
						.bindColorValue(ColorGenerator::lightThemeDesaturation) { ColorGenerator.lightThemeDesaturation = it }
					
					slider(0, 15, 1, 5)
						.label("Dark theme desaturation", LabelPosition.TOP)
						.enabledIf(generateColors.selected)
						.bindColorValue(ColorGenerator::darkThemeDesaturation) { ColorGenerator.darkThemeDesaturation = it }
					
					slider(0, 100, 5, 25)
						.label("Light theme threshold", LabelPosition.TOP)
						.enabledIf(generateColors.selected)
						.bindColorValue(ColorGenerator::lightThemeThreshold) { ColorGenerator.lightThemeThreshold = it }
					
					slider(0, 100, 5, 25)
						.label("Dark theme threshold", LabelPosition.TOP)
						.enabledIf(generateColors.selected)
						.bindColorValue(ColorGenerator::darkThemeThreshold) { ColorGenerator.darkThemeThreshold = it }
					
					slider(0, 25, 1, 5)
						.label("Light theme line alpha", LabelPosition.TOP)
						.enabledIf(generateColors.selected)
						.bindColorValue(ColorGenerator::lightThemeLineAlpha) { ColorGenerator.lightThemeLineAlpha = it }
					
					slider(0, 25, 1, 5)
						.label("Dark theme line alpha", LabelPosition.TOP)
						.enabledIf(generateColors.selected)
						.bindColorValue(ColorGenerator::darkThemeLineAlpha) { ColorGenerator.darkThemeLineAlpha = it }
				}
			}
		}
		
		private fun Row.createEditor(scheme: EditorColorsScheme) {
			val editor = createPreviewEditor(scheme, disposable)
			editorSyncer.add(editor)
			
			panel {
				row {
					cell(editor.component)
						.label(scheme.displayName, LabelPosition.TOP)
						.align(Align.FILL)
						.resizableColumn()
				}.resizableRow()
			}.resizableColumn()
		}
		
		private fun Cell<JSlider>.bindColorValue(getter: () -> Int, setter: (Int) -> Unit): Cell<JSlider> {
			return applyToComponent {
				value = getter()
				toolTipText = value.toString()
				
				addChangeListener {
					toolTipText = value.toString()
					setter(value)
					refreshColors()
				}
			}
		}
		
		private fun refreshColors() {
			for (severity in LensSeverity.values()) {
				severity.refreshColors()
			}
			
			for (editor in editorSyncer.editors) {
				recreateLenses(editor)
			}
		}
		
		override fun createActions(): Array<Action> {
			return arrayOf(okAction)
		}
		
		override fun getDimensionServiceKey(): String {
			return this::class.java.name
		}
	}
	
	private class EditorSyncer(private val disposable: Disposable) {
		val editors = mutableListOf<Editor>()
		
		fun add(editor: Editor) {
			editors.add(editor)
			
			editor.caretModel.addCaretListener(object : CaretListener {
				override fun caretPositionChanged(event: CaretEvent) {
					syncCarets(editor)
				}
				
				override fun caretAdded(event: CaretEvent) {
					syncCarets(editor)
				}
				
				override fun caretRemoved(event: CaretEvent) {
					syncCarets(editor)
				}
			}, disposable)
			
			editor.selectionModel.addSelectionListener(object : SelectionListener {
				override fun selectionChanged(e: SelectionEvent) {
					syncCarets(editor)
				}
			}, disposable)
		}
		
		private var isSyncing = false
		
		private fun syncCarets(mainEditor: Editor) {
			val caretsAndSelections = mainEditor.caretModel.caretsAndSelections
			
			syncEditors(mainEditor) {
				it.caretModel.caretsAndSelections = caretsAndSelections
			}
		}
		
		private inline fun syncEditors(mainEditor: Editor, action: (Editor) -> Unit) {
			if (isSyncing) {
				return
			}
			
			isSyncing = true
			try {
				for (editor in editors) {
					if (editor !== mainEditor) {
						action(editor)
					}
				}
			} finally {
				isSyncing = false
			}
		}
	}
	
	private companion object {
		private val SEVERITIES = listOf(
			HighlightSeverity.ERROR,
			HighlightSeverity.WARNING,
			HighlightSeverity.WEAK_WARNING,
			HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING,
			TextProblemSeverities.STYLE_SUGGESTION,
			SpellCheckerSeveritiesProvider.TYPO,
			HighlightSeverity("Other", 50)
		)
		
		private val LENSES_KEY = Key.create<MutableList<EditorLens>>("InspectionLens.ShowColorEditorAction.Lenses")
		
		private fun createPreviewEditor(scheme: EditorColorsScheme, disposable: Disposable): Editor {
			val editorFactory = EditorFactory.getInstance()
			
			val document = editorFactory.createDocument(('A'..'Z').take(SEVERITIES.size * 3).joinToString(separator = "\n", postfix = "\n\n") { "$it = 0;" })
			val editor = editorFactory.createViewer(document) as EditorEx
			
			editor.colorsScheme = scheme
			editor.caretModel.moveToOffset(editor.document.textLength)
			
			with(editor.settings) {
				additionalColumnsCount = 0
				additionalLinesCount = 0
				isIndentGuidesShown = false
				isLineMarkerAreaShown = false
				isLineNumbersShown = true
				isRightMarginShown = false
				isWhitespacesShown = true
				setGutterIconsShown(false)
			}
			
			ColorMode.setForEditor(editor, if (ColorUtil.isDark(scheme.defaultBackground)) ColorMode.ALWAYS_DARK else ColorMode.ALWAYS_LIGHT)
			recreateLenses(editor)
			
			Disposer.register(disposable) { editorFactory.releaseEditor(editor) }
			return editor
		}
		
		private fun recreateLenses(editor: Editor) {
			LENSES_KEY.get(editor)?.forEach(EditorLens::hide)
			
			val lenses = mutableListOf<EditorLens>()
			LENSES_KEY.set(editor, lenses)
			
			for ((index, severity) in SEVERITIES.withIndex()) {
				addLens(editor, severity, index, lenses)
				addLens(editor, severity, (index * 2) + 1 + SEVERITIES.size, lenses)
			}
		}
		
		private fun addLens(editor: Editor, severity: HighlightSeverity, line: Int, list: MutableList<EditorLens>) {
			val startOffset = editor.document.getLineStartOffset(line)
			val endOffset = editor.document.getLineEndOffset(line)
			
			val highlightInfo = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR)
				.severity(severity)
				.range(startOffset, endOffset)
				.descriptionAndTooltip("Example - ${severity.displayCapitalizedName}")
				.createUnconditionally()
			
			EditorLens.show(editor, highlightInfo, service())?.let(list::add)
		}
	}
}
