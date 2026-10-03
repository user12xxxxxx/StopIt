package com.nautesh.stopit

import java.time.LocalDate

/** One day's (or one bucket's) counts. */
data class DayCount(val pauses: Int = 0, val walkAways: Int = 0, val opened: Int = 0) {
    operator fun plus(other: DayCount) = DayCount(pauses + other.pauses, walkAways + other.walkAways, opened + other.opened)
}

/** The last [days] days ending [today], oldest first; days with no record count as zero. */
fun lastDays(history: Map<LocalDate, DayCount>, today: LocalDate, days: Int = 7): List<Pair<LocalDate, DayCount>> =
    (days - 1 downTo 0).map { back ->
        val day = today.minusDays(back.toLong())
        day to (history[day] ?: DayCount())
    }

/** The last [weeks] 7-day blocks ending [today], oldest first, each keyed by its first day. */
fun lastWeeks(history: Map<LocalDate, DayCount>, today: LocalDate, weeks: Int = 4): List<Pair<LocalDate, DayCount>> =
    (weeks - 1 downTo 0).map { back ->
        val end = today.minusDays(7L * back)
        val days = lastDays(history, end, 7)
        days.first().first to days.fold(DayCount()) { sum, (_, count) -> sum + count }
    }
