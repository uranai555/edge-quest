package com.edgequest.hero.reaction

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.edgequest.hero.data.SpeechFrequency
import com.edgequest.hero.data.model.LineCategory
import java.util.Calendar

/**
 * リアクションエンジン。
 * 時間帯・バッテリー・放置復帰・長時間使用・ランダムの各トリガーを監視し、
 * 条件成立時にSpeechManager経由で台詞を表示する。
 */
class ReactionEngine(
    private val context: Context,
    private val onTrigger: (LineCategory) -> Boolean
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastAppearTime = System.currentTimeMillis()
    private var cooldowns = mutableMapOf<LineCategory, Long>()

    // 設定値（外部から更新）
    var speechFrequency: SpeechFrequency = SpeechFrequency.Normal
    var batteryReactionEnabled: Boolean = true
    var timeReactionEnabled: Boolean = true
    var idleReturnReactionEnabled: Boolean = true
    var midnightReactionEnabled: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!batteryReactionEnabled) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            if (level in 1..20) {
                tryTrigger(LineCategory.LOW_BATTERY)
            }
        }
    }

    /**
     * 起動。タイマーとBroadcastReceiverを登録。
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        lastAppearTime = System.currentTimeMillis()

        // バッテリー監視（バッテリーが変化するたびにBroadcast）
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        // 時間帯チェック（5分ごと）
        mainHandler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return
                checkTimeReaction()
                checkLongUsage()
                checkRandomReaction()
                mainHandler.postDelayed(this, 5 * 60 * 1000L)
            }
        })

        // 即座に時間帯チェック
        checkTimeReaction()
    }

    /**
     * 停止。リソース解放。
     */
    fun stop() {
        isRunning = false
        try { context.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        mainHandler.removeCallbacksAndMessages(null)
        cooldowns.clear()
    }

    /**
     * 放置復帰検出用。OverlayServiceが表示ONになった時に呼ぶ。
     */
    fun onOverlayShown() {
        if (!idleReturnReactionEnabled) return
        val now = System.currentTimeMillis()
        val idleDuration = now - lastAppearTime
        if (idleDuration >= 30 * 60 * 1000L) { // 30分以上
            tryTrigger(LineCategory.IDLE_RETURN)
        }
        lastAppearTime = now
    }

    fun onOverlayHidden() {
        lastAppearTime = System.currentTimeMillis()
    }

    /**
     * レベルアップ通知用。GrowthManagerから呼ばれる。
     */
    fun onLevelUp() {
        tryTrigger(LineCategory.LEVEL_UP)
    }

    private fun checkTimeReaction() {
        if (!timeReactionEnabled) return
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val category = when (hour) {
            in 5..10 -> LineCategory.MORNING
            in 11..16 -> LineCategory.AFTERNOON
            in 17..20 -> LineCategory.EVENING
            in 21..23, in 0..4 -> {
                if (!midnightReactionEnabled) return
                LineCategory.NIGHT
            }
            else -> return
        }
        tryTrigger(category)
    }

    private fun checkLongUsage() {
        val elapsed = System.currentTimeMillis() - lastAppearTime
        if (elapsed >= 60 * 60 * 1000L) { // 60分以上連続表示
            tryTrigger(LineCategory.LONG_USAGE)
        }
    }

    private fun checkRandomReaction() {
        if (speechFrequency == SpeechFrequency.Low) return
        // 2時間に1回程度
        val lastRandom = cooldowns[LineCategory.RANDOM] ?: 0L
        if (System.currentTimeMillis() - lastRandom < 2 * 60 * 60 * 1000L) return
        tryTrigger(LineCategory.RANDOM)
    }

    private fun tryTrigger(category: LineCategory) {
        val now = System.currentTimeMillis()
        val lastTriggered = cooldowns[category] ?: 0L
        // カテゴリ共通クールダウン（安全マージン）
        if (now - lastTriggered < 5000) return

        val success = onTrigger(category)
        if (success) {
            cooldowns[category] = now
        }
    }
}