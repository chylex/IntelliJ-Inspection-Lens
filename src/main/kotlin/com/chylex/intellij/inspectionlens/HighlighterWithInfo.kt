package com.chylex.intellij.inspectionlens

import com.intellij.codeInsight.daemon.impl.AsyncDescriptionSupplier
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.markup.RangeHighlighter
import java.util.function.Consumer

open class HighlighterWithInfo private constructor(val highlighter: RangeHighlighter, private val info: HighlightInfo) {
	val hasDescription
		get() = info.description != null
	
	operator fun component1() = highlighter
	operator fun component2() = info
	
	class Async(highlighter: RangeHighlighter, info: HighlightInfo, private val provider: AsyncDescriptionSupplier) : HighlighterWithInfo(highlighter, info) {
		fun requestDescription(callback: Consumer<String?>) {
			provider.requestDescription().onSuccess(callback)
		}
	}
	
	companion object {
		fun from(highlighter: RangeHighlighter, info: HighlightInfo) =
			if (info is AsyncDescriptionSupplier)
				Async(highlighter, info, info)
			else
				HighlighterWithInfo(highlighter, info)
	}
}
