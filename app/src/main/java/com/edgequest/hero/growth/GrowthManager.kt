package com.edgequest.hero.growth

import com.edgequest.hero.data.HeroState
import com.edgequest.hero.data.HeroStateDataStore
import com.edgequest.hero.data.model.LineCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 勇者の育成管理（レベル・経験値・親密度・進化段階）。
 * GrowthManager.onEvent(eventType) を外部から呼ぶだけで自動計算・永続化する。
 */
class GrowthManager(
    private val heroStateDataStore: HeroStateDataStore,
    private val onLevelUp: () -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentState: HeroState = HeroState()

    /**
     * 現在の状態を読み込む（起動時など）。
     */
    suspend fun load(): HeroState {
        currentState = heroStateDataStore.state.first()
        return currentState
    }

    fun getState(): HeroState = currentState

    /**
     * 各種イベントに対する経験値・親密度の増加処理。
     */
    fun onEvent(eventType: EventType) {
        val expGain = eventType.expGain
        val intimacyGain = eventType.intimacyGain

        val newExp = currentState.exp + expGain
        var newLevel = currentState.level
        var newIntimacy = (currentState.intimacy + intimacyGain).coerceIn(0, 100)
        var overflowExp = newExp

        // レベルアップ処理
        while (true) {
            val expNeeded = newLevel * 50
            if (overflowExp >= expNeeded && newLevel < 10) {
                overflowExp -= expNeeded
                newLevel++
                onLevelUp()
            } else {
                break
            }
        }

        // 進化段階計算
        val newStage = when {
            newLevel >= 7 -> 3
            newLevel >= 3 -> 2
            else -> 1
        }

        currentState = currentState.copy(
            level = newLevel,
            exp = overflowExp,
            intimacy = newIntimacy,
            evolutionStage = newStage
        )

        // 非同期で保存
        scope.launch {
            heroStateDataStore.saveHeroState(currentState)
        }
    }

    enum class EventType(val expGain: Int, val intimacyGain: Int) {
        TAP(5, 1),
        EVENT(10, 2),
        IDLE_RETURN(15, 2)
    }
}
