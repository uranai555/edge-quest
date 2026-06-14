package com.edgequest.hero.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "edge_quest_settings"
)

enum class SpeechFrequency(val label: String) {
    Low("少ない"),
    Normal("普通"),
    High("多い");

    companion object {
        fun fromOrdinal(value: Int): SpeechFrequency =
            entries.getOrElse(value) { Normal }
    }
}

data class HeroSettings(
    val displayEnabled: Boolean = true,
    val characterSizeDp: Int = 48,
    val speechFrequency: SpeechFrequency = SpeechFrequency.Low,
    val batteryReactionEnabled: Boolean = true,
    val timeReactionEnabled: Boolean = true,
    val idleReturnReactionEnabled: Boolean = true,
    val midnightReactionEnabled: Boolean = false
)

class SettingsDataStore(private val context: Context) {
    val settings: Flow<HeroSettings> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            HeroSettings(
                displayEnabled = preferences[DISPLAY_ENABLED] ?: true,
                characterSizeDp = preferences[CHARACTER_SIZE_DP] ?: 48,
                speechFrequency = SpeechFrequency.fromOrdinal(
                    preferences[SPEECH_FREQUENCY] ?: SpeechFrequency.Normal.ordinal
                ),
                batteryReactionEnabled = preferences[BATTERY_REACTION_ENABLED] ?: true,
                timeReactionEnabled = preferences[TIME_REACTION_ENABLED] ?: true,
                idleReturnReactionEnabled = preferences[IDLE_RETURN_REACTION_ENABLED] ?: true,
                midnightReactionEnabled = preferences[MIDNIGHT_REACTION_ENABLED] ?: false
            )
        }

    suspend fun setDisplayEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[DISPLAY_ENABLED] = enabled }
    }

    suspend fun setCharacterSizeDp(sizeDp: Int) {
        context.settingsDataStore.edit { it[CHARACTER_SIZE_DP] = sizeDp }
    }

    suspend fun setSpeechFrequency(frequency: SpeechFrequency) {
        context.settingsDataStore.edit { it[SPEECH_FREQUENCY] = frequency.ordinal }
    }

    suspend fun setBatteryReactionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[BATTERY_REACTION_ENABLED] = enabled }
    }

    suspend fun setTimeReactionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[TIME_REACTION_ENABLED] = enabled }
    }

    suspend fun setIdleReturnReactionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[IDLE_RETURN_REACTION_ENABLED] = enabled }
    }

    suspend fun setMidnightReactionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[MIDNIGHT_REACTION_ENABLED] = enabled }
    }

    suspend fun clear() {
        context.settingsDataStore.edit { it.clear() }
    }

    private companion object {
        val DISPLAY_ENABLED = booleanPreferencesKey("display_enabled")
        val CHARACTER_SIZE_DP = intPreferencesKey("character_size_dp")
        val SPEECH_FREQUENCY = intPreferencesKey("speech_frequency")
        val BATTERY_REACTION_ENABLED = booleanPreferencesKey("battery_reaction_enabled")
        val TIME_REACTION_ENABLED = booleanPreferencesKey("time_reaction_enabled")
        val IDLE_RETURN_REACTION_ENABLED = booleanPreferencesKey("idle_return_reaction_enabled")
        val MIDNIGHT_REACTION_ENABLED = booleanPreferencesKey("midnight_reaction_enabled")
    }
}
