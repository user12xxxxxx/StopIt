package com.nautesh.stopit

/**
 * Decides when a foreground change needs the pause screen.
 *
 * An app the user chose to open stays allowed until they go to the launcher (home or
 * recents) or its time limit runs out, so switching between its own screens doesn't pause
 * it again. Allowances are per app: following a link from one allowed app into another
 * guarded app pauses that one, and going back doesn't pause the first again.
 */
class PauseGate(
    private val ownPackage: String,
    private val launchers: Set<String>,
    private val guarded: () -> Set<String>,
) {
    /** The package last seen in the foreground. */
    var current: String? = null
        private set
    private val allowedUntil = HashMap<String, Long>()

    fun allow(pkg: String, untilMillis: Long) {
        allowedUntil[pkg] = untilMillis
    }

    /** Ends every visit, so the next guarded app to resume is paused even if it was in front before. */
    fun reset() {
        current = null
        allowedUntil.clear()
    }

    /** Returns true when [pkg] just came to the foreground and should be paused. */
    fun onForeground(pkg: String): Boolean {
        if (pkg == current) return false
        current = pkg
        if (pkg in launchers) allowedUntil.clear()
        return pkg != ownPackage && pkg !in allowedUntil && pkg in guarded()
    }

    /** Returns true once, when the app in front is allowed and its time is up. */
    fun expired(nowMillis: Long): Boolean {
        val pkg = current ?: return false
        if (nowMillis < (allowedUntil[pkg] ?: return false)) return false
        allowedUntil.remove(pkg)
        return true
    }
}
