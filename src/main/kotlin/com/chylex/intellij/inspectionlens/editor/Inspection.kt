package com.chylex.intellij.inspectionlens.editor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.markup.RangeHighlighter

data class Inspection(val highlighter: RangeHighlighter, val info: HighlightInfo)
