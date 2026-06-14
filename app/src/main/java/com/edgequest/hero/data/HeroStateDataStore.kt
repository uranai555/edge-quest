package com.edgequest.hero.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.heroStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "edge_quest_hero_state"
)

data class HeroState(
    val level: Int = 1,
    val exp: Int = 0,
    val intimacy: Int = 0,
    val evolutionStage: Int = 1,
    val x: Int = 0,
    val y: Int = 160
)

class HeroStateDataStore(private val context: Context) {
    val state: Flow<HeroState> = context.heroStateDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            HeroState(
                level = preferences[LEVEL] ?: 1,
                exp = preferences[EXP] ?: 0,
                intimacy = preferences[INTIMACY] ?: 0,
                evolutionStage = preferences[EVOLUTION_STAGE] ?: 1,
                x = preferences[POSITION_X] ?: 0,
                y = preferences[POSITION_Y] ?: 160
            )
        }

    suspend fun saveHeroState(state: HeroState) {
        context.heroStateDataStore.edit {
            it[LEVEL] = state.level
            it[EXP] = state.exp
            it[INTIMACY] = state.intimacy
            it[EVOLUTION_STAGE] = state.evolutionStage
            it[POSITION_X] = state.x
            it[POSITION_Y] = state.y
        }
    }

    suspend fun savePosition(x: Int, y: Int) {
        context.heroStateDataStore.edit {
            it[POSITION_X] = x
            it[POSITION_Y] = y
        }
    }

    suspend fun resetPosition() {
        context.heroStateDataStore.edit {
            it.remove(POSITION_X)
            it.remove(POSITION_Y)
        }
    }

    suspend fun clear() {
        context.heroStateDataStore.edit { it.clear() }
    }

    private companion object {
        val LEVEL = intPreferencesKey("level")
        val EXP = intPreferencesKey("exp")
        val INTIMACY = intPreferencesKey("intimacy")
        val EVOLUTION_STAGE = intPreferencesKey("evolution_stage")
        val POSITION_X = intPreferencesKey("position_x")
        val POSITION_Y = intPreferencesKey("position_y")
    }
}
