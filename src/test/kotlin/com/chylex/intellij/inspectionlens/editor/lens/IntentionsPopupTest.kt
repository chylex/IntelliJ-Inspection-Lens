package com.chylex.intellij.inspectionlens.editor.lens

import com.intellij.codeInsight.intention.actions.ShowIntentionActionsAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntentionsPopupTest {
	@Test
	fun showIntentionActionsActionClassHasNotChanged() {
		assertEquals(IntentionsPopup.DEFAULT_ACTION_CLASS, ShowIntentionActionsAction::class.java.name)
	}
}
