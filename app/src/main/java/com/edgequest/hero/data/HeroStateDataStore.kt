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
                x = preferences[POSITION_X] ?: 0,
                y = preferences[POSITION_Y] ?: 160
            )
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
        val POSITION_X = intPreferencesKey("position_x")
        val POSITION_Y = intPreferencesKey("position_y")
    }
}
