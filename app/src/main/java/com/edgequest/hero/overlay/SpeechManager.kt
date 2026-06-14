package com.edgequest.hero.overlay

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.edgequest.hero.data.SpeechFrequency
import com.edgequest.hero.data.model.HeroLine
import com.edgequest.hero.data.model.LineCategory
import com.edgequest.hero.data.repository.HeroLineRepository

/**
 * 台詞表示管理システム。
 * クールダウン管理、isOneShot追跡、頻度設定反映を行う。
 */
class SpeechManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val heroParams: WindowManager.LayoutParams,
    private val heroSizePx: Int
) {
    private var currentBubble: SpeechBubbleView? = null
    private var evolutionStage: Int = 1
    private var speechFrequency: SpeechFrequency = SpeechFrequency.Normal
    private var oneShotUsedIds: MutableSet<Int> = mutableSetOf()

    // カテゴリ別クールダウン管理（最終表示時刻を保持）
    private val cooldowns: MutableMap<LineCategory, Long> = mutableMapOf()

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * タップ時の台詞表示エントリーポイント。
     */
    fun onTap() {
        val line = selectLine(LineCategory.TAP) ?: return
        showLine(line)
    }

    /**
     * 外部トリガー（リアクションエンジン）からの台詞表示。
     */
    fun onTrigger(category: LineCategory): Boolean {
        val line = selectLine(category) ?: return false
        showLine(line)
        return true
    }

    private fun selectLine(category: LineCategory): HeroLine? {
        val now = System.currentTimeMillis()

        // クールダウンチェック
        val lastTime = cooldowns[category] ?: 0L
        val line = HeroLineRepository.getRandomLine(category, evolutionStage) ?: return null
        val cooldownMs = line.cooldownSeconds * 1000L

        if (now - lastTime < cooldownMs) return null

        // isOneShotチェック
        if (line.isOneShot && line.id in oneShotUsedIds) {
            // 同じカテゴリで別の台詞を試す
            val alternatives = HeroLineRepository.getLinesByCategory(category, evolutionStage)
                .filter { !it.isOneShot || it.id !in oneShotUsedIds }
            val alt = alternatives.randomOrNull() ?: return null
            updateCooldown(category, now)
            markOneShot(alt)
            return alt
        }

        updateCooldown(category, now)
        markOneShot(line)
        return line
    }

    private fun updateCooldown(category: LineCategory, time: Long) {
        cooldowns[category] = time
    }

    private fun markOneShot(line: HeroLine) {
        if (line.isOneShot) oneShotUsedIds.add(line.id)
    }

    private fun showLine(line: HeroLine) {
        // 既存の吹き出しを非表示
        currentBubble?.dismiss()

        val bubble = SpeechBubbleView(context, windowManager, heroParams, heroSizePx)
        currentBubble = bubble
        bubble.showBubble(line.text)
    }

    /**
     * 進化段階を更新（台詞フィルタリングに使用）
     */
    fun updateEvolutionStage(stage: Int) {
        evolutionStage = stage
    }

    /**
     * 台詞頻度設定を更新
     */
    fun updateSpeechFrequency(frequency: SpeechFrequency) {
        speechFrequency = frequency
    }

    /**
     * 吹き出しを即座にクリア
     */
    fun dismissCurrentBubble() {
        currentBubble?.dismiss()
        currentBubble = null
    }

    /**
     * クリーンアップ
     */
    fun destroy() {
        dismissCurrentBubble()
        cooldowns.clear()
        oneShotUsedIds.clear()
    }
}