package com.chylex.intellij.inspectionlens.settings

import com.chylex.intellij.inspectionlens.editor.lens.LensSeverityFilter
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ConfigurableWithId
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DisabledTraversalPolicy
import com.intellij.ui.EditorTextFieldCellRenderer.SimpleRendererComponent
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import java.awt.Cursor

class LensApplicationConfigurable : BoundConfigurable("Inspection Lens"), ConfigurableWithId {
	companion object {
		const val ID = "InspectionLens"
	}
	
	private data class DisplayedSeverity(
		val id: String,
		val severity: StoredSeverity,
		val textAttributes: TextAttributes? = null,
	) {
		constructor(
			severity: HighlightSeverity,
			registrar: SeverityRegistrar,
		) : this(
			id = severity.name,
			severity = StoredSeverity(severity),
			textAttributes = registrar.getHighlightInfoTypeBySeverity(severity).attributesKey.defaultAttributes
		)
	}
	
	private val settingsService = service<LensSettingsState>()
	
	private val allSeverities by lazy(LazyThreadSafetyMode.NONE) {
		val settings = settingsService.state
		val registrar = SeverityRegistrar.getSeverityRegistrar(null)
		
		val knownSeverities = LensSeverityFilter.getSupportedSeverities(registrar).map { DisplayedSeverity(it, registrar) }
		val knownSeverityIds = knownSeverities.mapTo(HashSet(), DisplayedSeverity::id)
		
		// Update names and priorities of stored severities.
		for ((id, knownSeverity, _) in knownSeverities) {
			val storedSeverity = settings.hiddenSeverities[id]
			if (storedSeverity != null && storedSeverity != knownSeverity) {
				settings.hiddenSeverities[id] = knownSeverity
			}
		}
		
		val unknownSeverities = settings.hiddenSeverities.entries
			.filterNot { it.key in knownSeverityIds }
			.map { DisplayedSeverity(it.key, it.value) }
		
		(knownSeverities + unknownSeverities).sortedByDescending { it.severity.priority }
	}
	
	override fun getId(): String {
		return ID
	}
	
	override fun createPanel(): DialogPanel {
		val settings = settingsService.state
		
		return panel {
			group("Appearance") {
				row {
					checkBox("Use editor font").bindSelected(settings::useEditorFont)
				}
			}
			
			group("Behavior") {
				row("Hover mode:") {
					val items = LensHoverMode.values().toList()
					val renderer = SimpleListCellRenderer.create("", LensHoverMode::description)
					comboBox(items, renderer).bindItem(settings::lensHoverMode) { settings.lensHoverMode = it ?: LensHoverMode.DEFAULT }
				}
			}
			
			group("Shown Severities") {
				for ((id, severity, textAttributes) in allSeverities) {
					row {
						checkBox(severity.name)
							.bindSelectedToNotIn(settings.hiddenSeverities, id, severity)
							.gap(RightGap.COLUMNS)
						
						labelWithAttributes("Example", textAttributes)
					}.layout(RowLayout.PARENT_GRID)
				}
				
				row {
					checkBox("Other").bindSelected(settings::showUnknownSeverities)
				}
			}
		}
	}
	
	private fun <K, V> Cell<JBCheckBox>.bindSelectedToNotIn(collection: MutableMap<K, V>, key: K, value: V): Cell<JBCheckBox> {
		return bindSelected({ key !in collection }, { if (it) collection.remove(key) else collection[key] = value })
	}
	
	private fun Row.labelWithAttributes(text: String, textAttributes: TextAttributes?): Cell<SimpleRendererComponent> {
		val label = SimpleRendererComponent(null, null, true)
		label.setText(text, textAttributes, false)
		label.focusTraversalPolicy = DisabledTraversalPolicy()
		
		val editor = label.editor
		editor.setCustomCursor(this, Cursor.getDefaultCursor())
		editor.contentComponent.setOpaque(false)
		editor.selectionModel.addSelectionListener(object : SelectionListener {
			override fun selectionChanged(e: SelectionEvent) {
				if (!e.newRange.isEmpty) {
					editor.selectionModel.removeSelection(true)
				}
			}
		})
		
		Disposer.register(disposable!!, label)
		return cell(label)
	}
	
	override fun apply() {
		super.apply()
		settingsService.update()
	}
}
