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
import com.edgequest.hero.data.HeroStateDataStore
import com.edgequest.hero.data.model.LineCategory
import com.edgequest.hero.overlay.HeroOverlayManager
import com.edgequest.hero.overlay.SpeechManager
import com.edgequest.hero.reaction.ReactionEngine
import com.edgequest.hero.growth.GrowthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private var overlayManager: HeroOverlayManager? = null
    private var speechManager: SpeechManager? = null
    private var reactionEngine: ReactionEngine? = null
    private var growthManager: GrowthManager? = null
    private var heroDataStore: HeroStateDataStore? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        heroDataStore = HeroStateDataStore(applicationContext)
        speechManager = SpeechManager(
            context = this,
            windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager,
            heroParams = android.view.WindowManager.LayoutParams(),
            heroSizePx = 48
        )
        overlayManager = HeroOverlayManager(
            context = this,
            onHeroTap = {
                speechManager?.onTap()
                growthManager?.onEvent(GrowthManager.EventType.TAP)
            }
        )
        reactionEngine = ReactionEngine(
            context = this,
            onTrigger = { category ->
                val shown = speechManager?.onTrigger(category) ?: false
                if (shown) {
                    val event = when (category) {
                        LineCategory.IDLE_RETURN -> GrowthManager.EventType.IDLE_RETURN
                        else -> GrowthManager.EventType.EVENT
                    }
                    growthManager?.onEvent(event)
                }
                shown
            }
        )
        growthManager = GrowthManager(
            heroStateDataStore = heroDataStore!!,
            onLevelUp = {
                reactionEngine?.onLevelUp()
                CoroutineScope(Dispatchers.Main).launch {
                    val state = heroDataStore!!.state.first()
                    speechManager?.updateEvolutionStage(state.evolutionStage)
                }
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                overlayManager?.hide()
                reactionEngine?.stop()
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
                reactionEngine?.start()
                reactionEngine?.onOverlayShown()
                CoroutineScope(Dispatchers.IO).launch {
                    growthManager?.load()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        speechManager?.destroy()
        reactionEngine?.stop()
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