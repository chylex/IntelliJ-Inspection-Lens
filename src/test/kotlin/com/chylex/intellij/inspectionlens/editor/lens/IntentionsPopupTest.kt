package com.chylex.intellij.inspectionlens.editor.lens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntentionsPopupTest {
	@Test
	fun hasShowPopupMethod() {
		assertTrue(IntentionsPopup.hasShowPopupMethod)
	}
}
