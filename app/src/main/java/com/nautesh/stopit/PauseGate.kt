package com.nautesh.stopit

/**
 * Decides when a foreground change needs the pause screen.
 *
 * An app the user chose to open stays allowed until they leave it or its time limit
 * runs out, so switching between its own screens doesn't pause it again.
 */
class PauseGate(private val ownPackage: String, private val guarded: () -> Set<String>) {
    /** The package last seen in the foreground. */
    var current: String? = null
        private set
    private var allowed: String? = null
    private var allowedUntil = 0L

    fun allow(pkg: String, untilMillis: Long) {
        allowed = pkg
        allowedUntil = untilMillis
    }

    /** Returns true when [pkg] just came to the foreground and should be paused. */
    fun onForeground(pkg: String): Boolean {
        if (pkg == current) return false
        current = pkg
        if (pkg == ownPackage || pkg == allowed) return false
        allowed = null
        return pkg in guarded()
    }

    /** Returns true once, when the allowed app is still in front and its time is up. */
    fun expired(nowMillis: Long): Boolean {
        if (allowed == null || allowed != current || nowMillis < allowedUntil) return false
        allowed = null
        return true
    }
}
