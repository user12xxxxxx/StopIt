package com.nautesh.stopit

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/** Watches which app is in the foreground and shows the pause screen over guarded ones. */
class GuardService : Service() {

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, GuardService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }

        // Same process, main thread only: PauseActivity calls allow() through this.
        var gate: PauseGate? = null
            private set

        // ponytail: polls usage events twice a second; switch to an AccessibilityService if this lags or drains battery.
        private const val POLL_MS = 500L
        private const val CHANNEL = "guard"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usage: UsageStatsManager
    private lateinit var prefs: Prefs
    private lateinit var power: PowerManager
    private lateinit var keyguard: KeyguardManager
    private var since = 0L
    private var locked = false

    private val poll = object : Runnable {
        override fun run() {
            check()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usage = getSystemService(UsageStatsManager::class.java)
        prefs = Prefs(this)
        power = getSystemService(PowerManager::class.java)
        keyguard = getSystemService(KeyguardManager::class.java)
        gate = PauseGate(packageName) { prefs.guarded }.apply {
            // Timed visits from before a restart still count.
            prefs.liveAllowances(System.currentTimeMillis()).forEach { (pkg, until) -> allow(pkg, until) }
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Guard", NotificationManager.IMPORTANCE_MIN))
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("StopIt is guarding your apps")
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, 1, notification, type)

        since = System.currentTimeMillis()
        handler.post(poll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        gate = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun check() {
        val now = System.currentTimeMillis()
        // Never pause behind the lock screen. Locking ends visits with no limit, so unlocking pauses the app again.
        if (!power.isInteractive || keyguard.isKeyguardLocked) {
            locked = true
            return
        }
        if (locked) gate?.reset()
        locked = false
        val events = usage.queryEvents(since, now)
        since = now
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) latest = event.packageName
        }
        val gate = gate ?: return
        when {
            latest != null && gate.onForeground(latest, now) -> pause(latest)
            // The visit's time limit ran out while the app is still open.
            gate.expired(now) -> gate.current?.let(::pause)
        }
    }

    private fun pause(pkg: String) {
        prefs.recordPause(pkg)
        PauseActivity.start(this, pkg)
    }
}

/** Restarts the guard after a reboot if the user left it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs(context).guarding) GuardService.start(context)
    }
}
