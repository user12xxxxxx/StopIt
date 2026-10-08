package com.nautesh.stopit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseGateTest {
    private val gate = PauseGate("me", setOf("launcher")) { setOf("insta", "tiktok") }

    @Test
    fun detourThroughOtherAppKeepsTheVisit() {
        gate.onForeground("insta", 0)
        gate.onForeground("me", 0)
        gate.allow("insta", untilMillis = 1_000)
        gate.onForeground("insta", 0)
        assertFalse(gate.onForeground("chrome", 0))
        assertFalse(gate.onForeground("insta", 0))
        assertTrue(gate.expired(1_000))
    }

    @Test
    fun allowancesArePerApp() {
        gate.onForeground("insta", 0)
        gate.onForeground("me", 0)
        gate.allow("insta", untilMillis = PauseGate.NO_LIMIT)
        gate.onForeground("insta", 0)
        assertTrue(gate.onForeground("tiktok", 0))
        gate.onForeground("me", 0)
        gate.allow("tiktok", untilMillis = PauseGate.NO_LIMIT)
        assertFalse(gate.onForeground("tiktok", 0))
        assertFalse(gate.onForeground("insta", 0))
        gate.onForeground("launcher", 0)
        assertTrue(gate.onForeground("insta", 0))
    }

    @Test
    fun resetPausesTheAppAlreadyInFront() {
        gate.onForeground("insta", 0)
        gate.onForeground("me", 0)
        gate.allow("insta", untilMillis = PauseGate.NO_LIMIT)
        gate.onForeground("insta", 0)
        gate.reset()
        assertTrue(gate.onForeground("insta", 0))
    }

    @Test
    fun pausesGuardedAppOnlyWhenItArrives() {
        assertFalse(gate.onForeground("launcher", 0))
        assertTrue(gate.onForeground("insta", 0))
        assertFalse(gate.onForeground("insta", 0))
    }

    @Test
    fun noLimitVisitLastsUntilUserLeaves() {
        assertTrue(gate.onForeground("insta", 0))
        assertFalse(gate.onForeground("me", 0))
        gate.allow("insta", untilMillis = PauseGate.NO_LIMIT)
        assertFalse(gate.onForeground("insta", 0))
        assertFalse(gate.onForeground("launcher", 0))
        assertTrue(gate.onForeground("insta", 0))
    }

    @Test
    fun timedVisitSurvivesLeavingUntilItsTimeIsUp() {
        gate.onForeground("insta", 0)
        gate.onForeground("me", 0)
        gate.allow("insta", untilMillis = 1_000)
        gate.onForeground("insta", 0)
        gate.onForeground("launcher", 100)
        assertFalse(gate.onForeground("insta", 500))
        gate.onForeground("launcher", 600)
        gate.reset()
        assertFalse(gate.onForeground("insta", 900))
        gate.onForeground("launcher", 950)
        assertTrue(gate.onForeground("insta", 1_000))
    }

    @Test
    fun returningFromPauseScreenWithoutAllowingPausesAgain() {
        assertTrue(gate.onForeground("insta", 0))
        assertFalse(gate.onForeground("me", 0))
        assertTrue(gate.onForeground("insta", 0))
    }

    @Test
    fun timeLimitExpiresOnceWhileAppIsInFront() {
        gate.onForeground("insta", 0)
        gate.onForeground("me", 0)
        gate.allow("insta", untilMillis = 1_000)
        gate.onForeground("insta", 0)
        assertFalse(gate.expired(999))
        assertTrue(gate.expired(1_000))
        assertFalse(gate.expired(2_000))
    }

    @Test
    fun timeLimitDoesNotFireAfterUserLeft() {
        gate.onForeground("insta", 0)
        gate.onForeground("me", 0)
        gate.allow("insta", untilMillis = 1_000)
        gate.onForeground("insta", 0)
        gate.onForeground("launcher", 0)
        assertFalse(gate.expired(5_000))
    }
}
