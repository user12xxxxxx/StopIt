package com.nautesh.stopit

/**
 * Decides when a foreground change needs the pause screen.
 *
 * An app the user chose to open with a time limit stays allowed until that time is up, even if
 * they leave it and come back. One opened with no limit stays allowed until the phone is locked
 * ([reset]), even through the launcher. Either way, switching between its own screens doesn't pause it
 * again. Allowances are per app: following a link from one allowed app into another guarded
 * app pauses that one, and going back doesn't pause the first again.
 */
class PauseGate(
    private val ownPackage: String,
    private val guarded: () -> Set<String>,
) {
    companion object {
        /** An allowance with no time limit: it lasts until the phone is locked. */
        const val NO_LIMIT = Long.MAX_VALUE
    }

    /** The package last seen in the foreground. */
    var current: String? = null
        private set
    private val allowedUntil = HashMap<String, Long>()

    fun allow(pkg: String, untilMillis: Long) {
        allowedUntil[pkg] = untilMillis
    }

    /**
     * Ends visits without a time limit, so the next guarded app to resume is paused even if it was in
     * front before. Timed visits keep running.
     */
    fun reset() {
        current = null
        allowedUntil.values.removeAll { it == NO_LIMIT }
    }

    /** Returns true when [pkg] just came to the foreground and should be paused. */
    fun onForeground(pkg: String, nowMillis: Long): Boolean {
        if (pkg == current) return false
        current = pkg
        return pkg != ownPackage && pkg in guarded() && nowMillis >= (allowedUntil[pkg] ?: 0L)
    }

    /** Returns true once, when the app in front is allowed and its time is up. */
    fun expired(nowMillis: Long): Boolean {
        val pkg = current ?: return false
        if (nowMillis < (allowedUntil[pkg] ?: return false)) return false
        allowedUntil.remove(pkg)
        return true
    }
}
