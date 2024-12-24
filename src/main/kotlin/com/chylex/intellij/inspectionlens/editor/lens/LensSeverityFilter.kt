package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.lang.annotation.HighlightSeverity
import java.util.function.Predicate

class LensSeverityFilter(private val hiddenSeverityIds: Set<String>, private val showUnknownSeverities: Boolean) : Predicate<HighlightSeverity> {
	private val knownSeverityIds = getSupportedSeverities().mapTo(HashSet(), HighlightSeverity::getName)
	
	override fun test(severity: HighlightSeverity): Boolean {
		if (!isSupported(severity)) {
			return false
		}
		
		return if (severity.name in knownSeverityIds)
			severity.name !in hiddenSeverityIds
		else
			showUnknownSeverities
	}
	
	companion object {
		@Suppress("DEPRECATION")
		private fun isSupported(severity: HighlightSeverity): Boolean {
			return severity > HighlightSeverity.TEXT_ATTRIBUTES && severity !== HighlightSeverity.INFO
		}
		
		fun getSupportedSeverities(registrar: SeverityRegistrar = SeverityRegistrar.getSeverityRegistrar(null)): List<HighlightSeverity> {
			return registrar.allSeverities.filter(::isSupported)
		}
	}
}
