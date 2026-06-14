package com.edgequest.hero.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.edgequest.hero.R
import com.edgequest.hero.overlay.HeroOverlayManager

class OverlayService : Service() {

    private var overlayManager: HeroOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayManager = HeroOverlayManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                overlayManager?.hide()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_SIZE -> {
                val sizeDp = intent.getIntExtra(EXTRA_SIZE_DP, 48)
                overlayManager?.updateSize(sizeDp)
                return START_STICKY
            }
            ACTION_HIDE -> {
                overlayManager?.hide()
                return START_STICKY
            }
            ACTION_SHOW -> {
                val sizeDp = intent.getIntExtra(EXTRA_SIZE_DP, 48)
                val savedX = if (intent.hasExtra(EXTRA_POS_X))
                    intent.getIntExtra(EXTRA_POS_X, -1) else null
                val savedY = if (intent.hasExtra(EXTRA_POS_Y))
                    intent.getIntExtra(EXTRA_POS_Y, -1) else null
                overlayManager?.show(sizeDp, savedX, savedY)
                return START_STICKY
            }
            else -> {
                // ACTION_START or default
                startForeground(NOTIFICATION_ID, buildNotification())
                val sizeDp = intent?.getIntExtra(EXTRA_SIZE_DP, 48) ?: 48
                val savedX = if (intent?.hasExtra(EXTRA_POS_X) == true)
                    intent.getIntExtra(EXTRA_POS_X, -1) else null
                val savedY = if (intent?.hasExtra(EXTRA_POS_Y) == true)
                    intent.getIntExtra(EXTRA_POS_Y, -1) else null
                overlayManager?.show(sizeDp, savedX, savedY)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        overlayManager?.hide()
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_service_notification))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.edgequest.hero.action.START_OVERLAY"
        const val ACTION_STOP = "com.edgequest.hero.action.STOP_OVERLAY"
        const val ACTION_UPDATE_SIZE = "com.edgequest.hero.action.UPDATE_SIZE"
        const val ACTION_HIDE = "com.edgequest.hero.action.HIDE_OVERLAY"
        const val ACTION_SHOW = "com.edgequest.hero.action.SHOW_OVERLAY"

        const val EXTRA_SIZE_DP = "size_dp"
        const val EXTRA_POS_X = "pos_x"
        const val EXTRA_POS_Y = "pos_y"

        private const val CHANNEL_ID = "edge_quest_overlay"
        private const val NOTIFICATION_ID = 1001
    }
}