package com.nautesh.stopit

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsTest {
    private val today = LocalDate.of(2026, 10, 3)

    @Test
    fun lastDaysFillsGapsAndEndsToday() {
        val history = mapOf(today to DayCount(3, 1), today.minusDays(2) to DayCount(5, 2))
        val days = lastDays(history, today, 3)
        assertEquals(listOf(today.minusDays(2), today.minusDays(1), today), days.map { it.first })
        assertEquals(listOf(DayCount(5, 2), DayCount(), DayCount(3, 1)), days.map { it.second })
    }

    @Test
    fun lastWeeksSumsSevenDayBlocks() {
        val history = mapOf(
            today to DayCount(1, 1),
            today.minusDays(6) to DayCount(2, 0), // same block as today
            today.minusDays(7) to DayCount(10, 4), // previous block
            today.minusDays(28) to DayCount(99, 99), // outside 4 weeks
        )
        val weeks = lastWeeks(history, today, 4)
        assertEquals(today.minusDays(6), weeks.last().first)
        assertEquals(listOf(DayCount(), DayCount(), DayCount(10, 4), DayCount(3, 1)), weeks.map { it.second })
    }
}
