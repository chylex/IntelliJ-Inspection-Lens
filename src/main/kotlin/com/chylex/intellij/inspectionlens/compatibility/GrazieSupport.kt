package com.chylex.intellij.inspectionlens.compatibility

import com.chylex.intellij.inspectionlens.editor.lens.LensSeverity
import com.intellij.grazie.ide.TextProblemSeverities
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class GrazieSupport : ProjectActivity {
	override suspend fun execute(project: Project) {
		LensSeverity.registerMapping(TextProblemSeverities.GRAMMAR_ERROR, LensSeverity.ERROR)
		LensSeverity.registerMapping(TextProblemSeverities.STYLE_ERROR, LensSeverity.GRAZIE)
		LensSeverity.registerMapping(TextProblemSeverities.STYLE_WARNING, LensSeverity.GRAZIE)
		LensSeverity.registerMapping(TextProblemSeverities.STYLE_SUGGESTION, LensSeverity.GRAZIE)
	}
}
