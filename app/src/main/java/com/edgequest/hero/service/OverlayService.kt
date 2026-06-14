package com.edgequest.hero.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.edgequest.hero.MainActivity
import com.edgequest.hero.R
import com.edgequest.hero.data.HeroStateDataStore
import com.edgequest.hero.data.SettingsDataStore
import com.edgequest.hero.data.model.LineCategory
import com.edgequest.hero.growth.GrowthManager
import com.edgequest.hero.overlay.HeroOverlayManager
import com.edgequest.hero.overlay.SpeechManager
import com.edgequest.hero.reaction.ReactionEngine
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
            ACTION_STOP_FROM_NOTIFICATION -> {
                // 通知欄から「休ませる」が押された
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
                reactionEngine?.start()
                reactionEngine?.onOverlayShown()
                return START_STICKY
            }
            ACTION_TEST_TRIGGER -> {
                val type = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: ""
                val category = when (type) {
                    "battery" -> LineCategory.LOW_BATTERY
                    "night" -> LineCategory.NIGHT
                    "idle" -> LineCategory.IDLE_RETURN
                    "long" -> LineCategory.LONG_USAGE
                    else -> LineCategory.TAP
                }
                speechManager?.onTrigger(category)
                return START_STICKY
            }
            ACTION_SET_STAGE -> {
                val stage = intent.getIntExtra(EXTRA_STAGE, 1).coerceIn(1, 3)
                overlayManager?.setEvolutionStage(stage)
                return START_STICKY
            }
            ACTION_RESET_COOLDOWNS -> {
                reactionEngine?.stop()
                reactionEngine?.start()
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

    private fun buildNotification(): Notification {
        // アプリを開くIntent
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 休ませるIntent
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_FROM_NOTIFICATION
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("タップして設定 / 停止できます")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "休ませる",
                stopPendingIntent
            )
            .build()
    }

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
        const val ACTION_STOP_FROM_NOTIFICATION = "com.edgequest.hero.action.STOP_FROM_NOTIFICATION"
        const val ACTION_UPDATE_SIZE = "com.edgequest.hero.action.UPDATE_SIZE"
        const val ACTION_HIDE = "com.edgequest.hero.action.HIDE_OVERLAY"
        const val ACTION_SHOW = "com.edgequest.hero.action.SHOW_OVERLAY"
        const val ACTION_TEST_TRIGGER = "com.edgequest.hero.action.TEST_TRIGGER"
        const val ACTION_SET_STAGE = "com.edgequest.hero.action.SET_STAGE"
        const val ACTION_RESET_COOLDOWNS = "com.edgequest.hero.action.RESET_COOLDOWNS"

        const val EXTRA_SIZE_DP = "size_dp"
        const val EXTRA_POS_X = "pos_x"
        const val EXTRA_POS_Y = "pos_y"
        const val EXTRA_TRIGGER_TYPE = "trigger_type"
        const val EXTRA_STAGE = "stage"

        private const val CHANNEL_ID = "edge_quest_overlay"
        private const val NOTIFICATION_ID = 1001
    }
}