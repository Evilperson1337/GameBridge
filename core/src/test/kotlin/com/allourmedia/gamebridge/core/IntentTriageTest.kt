package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentTriageTest {
    /** [scheme] null means no data at all. */
    private fun triage(
        action: String?,
        scheme: String?,
        hasData: Boolean = scheme != null,
    ) = triageIntent(action, scheme, hasData)

    private fun failed(category: FailureCategory) = IntentTriage.Failed(category)

    @Test
    fun withoutDataMainNullAndOwnActionAreNoShortcut() {
        for (action in listOf(null, MAIN_ACTION, DISPATCH_ACTION)) {
            assertEquals(action, failed(FailureCategory.NoShortcut), triage(action, null))
        }
    }

    @Test
    fun withoutDataAnyOtherActionIsWrongAction() {
        assertEquals(failed(FailureCategory.WrongAction), triage("android.intent.action.VIEW", null))
    }

    @Test
    fun withDataAnyOtherActionIsWrongActionEvenForMain() {
        for (action in listOf(null, MAIN_ACTION, "android.intent.action.VIEW")) {
            assertEquals(action, failed(FailureCategory.WrongAction), triage(action, "content"))
        }
    }

    @Test
    fun ownActionWithANonContentSchemeIsNotContentUri() {
        for (scheme in listOf("file", "http", "", "CONTENT")) {
            assertEquals(scheme, failed(FailureCategory.NotContentUri), triage(DISPATCH_ACTION, scheme))
        }
        // Data present but without a scheme at all (a relative URI).
        assertEquals(failed(FailureCategory.NotContentUri), triage(DISPATCH_ACTION, null, hasData = true))
    }

    @Test
    fun ownActionWithContentSchemeIsAccepted() {
        assertEquals(IntentTriage.Accepted, triage(DISPATCH_ACTION, "content"))
    }
}
