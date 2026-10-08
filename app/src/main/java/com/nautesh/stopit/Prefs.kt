package com.nautesh.stopit

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

/** User settings. SharedPreferences caches them in memory, so the service can read them on every check. */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var guarded: Set<String>
        get() = prefs.getStringSet("guarded", null)?.toSet() ?: DEFAULT_GUARDED
        set(value) = prefs.edit { putStringSet("guarded", value) }

    var minSeconds: Int
        get() = prefs.getInt("min_seconds", 3)
        set(value) = prefs.edit { putInt("min_seconds", value) }

    var maxSeconds: Int
        get() = prefs.getInt("max_seconds", 15)
        set(value) = prefs.edit { putInt("max_seconds", value) }

    var message: String
        get() = prefs.getString("message", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_MESSAGE
        set(value) = prefs.edit { putString("message", value) }

    /** Pure black pause screen, for AMOLED displays. */
    var amoled: Boolean
        get() = prefs.getBoolean("amoled", false)
        set(value) = prefs.edit { putBoolean("amoled", value) }

    /** Light, dark, or following the system setting. */
    var theme: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }.getOrDefault(ThemeMode.System)
        set(value) = prefs.edit { putString("theme", value.name) }

    /** Whether the user wants the guard running; the boot receiver and Home read it. */
    var guarding: Boolean
        get() = prefs.getBoolean("guarding", true)
        set(value) = prefs.edit { putBoolean("guarding", value) }

    // Daily history for Home's counters and trend chart, one "hist:<date>" key per day holding "pauses,walkAways,opened".
    val history: Map<LocalDate, DayCount>
        get() = prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(HIST)) return@mapNotNull null
            // Days saved before "opened" was tracked have two fields; the third then counts as 0.
            val n = (value as String).split(',').map(String::toInt)
            LocalDate.parse(key.removePrefix(HIST)) to DayCount(n[0], n[1], n.getOrElse(2) { 0 })
        }.toMap()

    private fun today() = history[LocalDate.now()] ?: DayCount()
    val walkAwaysToday: Int get() = today().walkAways
    val openedToday: Int get() = today().opened

    /** How many times [pkg] was paused today, including the current visit. */
    fun visitsToday(pkg: String) = todayCount("visits:$pkg")

    fun recordPause(pkg: String) {
        addToday(DayCount(pauses = 1))
        bump("visits:$pkg")
    }

    fun recordWalkAway() = addToday(DayCount(walkAways = 1))

    /** The user went past the pause and opened the app. */
    fun recordOpened() = addToday(DayCount(opened = 1))

    private fun addToday(add: DayCount) {
        val now = LocalDate.now()
        val count = today() + add
        prefs.edit {
            putString(HIST + now, "${count.pauses},${count.walkAways},${count.opened}")
            // The chart shows at most 4 weeks, so older days are dropped.
            prefs.all.keys
                .filter { it.startsWith(HIST) && LocalDate.parse(it.removePrefix(HIST)) < now.minusDays(HISTORY_DAYS) }
                .forEach { remove(it) }
        }
    }

    private fun isToday() = prefs.getString("stats_date", null) == LocalDate.now().toString()

    private fun todayCount(key: String) = if (isToday()) prefs.getInt(key, 0) else 0

    private fun bump(vararg keys: String) {
        val newDay = !isToday()
        val counts = keys.associateWith { todayCount(it) + 1 }
        prefs.edit {
            if (newDay) {
                putString("stats_date", LocalDate.now().toString())
                prefs.all.keys
                    .filter { it.startsWith("visits:") }
                    .forEach { remove(it) }
            }
            counts.forEach { (key, count) -> putInt(key, count) }
        }
    }

    /**
     * Saves a timed visit ("Open for 5 min"), so it outlasts the guard service: Android may kill and restart it, for
     * example when the user clears apps from Recents, and the service's allowances live in memory.
     */
    fun saveAllowance(pkg: String, untilMillis: Long) = prefs.edit { putLong(ALLOW + pkg, untilMillis) }

    /** Timed visits still running at [nowMillis]; ones that have run out are deleted. */
    fun liveAllowances(nowMillis: Long): Map<String, Long> {
        val saved = prefs.all.filterKeys { it.startsWith(ALLOW) }.mapValues { it.value as? Long ?: 0L }
        prefs.edit { saved.filterValues { it <= nowMillis }.keys.forEach(::remove) }
        return saved.filterValues { it > nowMillis }.mapKeys { it.key.removePrefix(ALLOW) }
    }

    companion object {
        private const val ALLOW = "allow:"
        private const val HIST = "hist:"
        private const val HISTORY_DAYS = 28L

        val DEFAULT_GUARDED = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.zhiliaoapp.musically",
            "com.twitter.android",
        )
        const val DEFAULT_MESSAGE = "What were you about to look for?"
    }
}

enum class ThemeMode(val label: String) { System("System"), Light("Light"), Dark("Dark") }
