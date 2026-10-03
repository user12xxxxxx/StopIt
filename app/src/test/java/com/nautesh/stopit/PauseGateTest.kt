package com.nautesh.stopit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseGateTest {
    private val gate = PauseGate("me") { setOf("insta") }

    @Test
    fun pausesGuardedAppOnlyWhenItArrives() {
        assertFalse(gate.onForeground("launcher"))
        assertTrue(gate.onForeground("insta"))
        assertFalse(gate.onForeground("insta"))
    }

    @Test
    fun allowedAppStaysOpenUntilUserLeavesIt() {
        assertTrue(gate.onForeground("insta"))
        assertFalse(gate.onForeground("me"))
        gate.allow("insta", untilMillis = 1_000)
        assertFalse(gate.onForeground("insta"))
        assertFalse(gate.onForeground("launcher"))
        assertTrue(gate.onForeground("insta"))
    }

    @Test
    fun returningFromPauseScreenWithoutAllowingPausesAgain() {
        assertTrue(gate.onForeground("insta"))
        assertFalse(gate.onForeground("me"))
        assertTrue(gate.onForeground("insta"))
    }

    @Test
    fun timeLimitExpiresOnceWhileAppIsInFront() {
        gate.onForeground("insta")
        gate.onForeground("me")
        gate.allow("insta", untilMillis = 1_000)
        gate.onForeground("insta")
        assertFalse(gate.expired(999))
        assertTrue(gate.expired(1_000))
        assertFalse(gate.expired(2_000))
    }

    @Test
    fun timeLimitDoesNotFireAfterUserLeft() {
        gate.onForeground("insta")
        gate.onForeground("me")
        gate.allow("insta", untilMillis = 1_000)
        gate.onForeground("insta")
        gate.onForeground("launcher")
        assertFalse(gate.expired(5_000))
    }
}
