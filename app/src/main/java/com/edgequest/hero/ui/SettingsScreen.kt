package com.edgequest.hero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgequest.hero.data.HeroSettings
import com.edgequest.hero.data.HeroStateDataStore
import com.edgequest.hero.data.SettingsDataStore
import com.edgequest.hero.data.SpeechFrequency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    heroStateDataStore: HeroStateDataStore,
    onDisplayEnabledChanged: (Boolean) -> Unit,
    onSizeChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val settings by settingsDataStore.settings.collectAsState(initial = HeroSettings())
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            SettingSwitchRow(
                title = "表示ON/OFF",
                checked = settings.displayEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        settingsDataStore.setDisplayEnabled(enabled)
                        onDisplayEnabledChanged(enabled)
                    }
                }
            )

            OptionSection(title = "キャラサイズ") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(36, 48, 64).forEach { size ->
                        FilterChip(
                            selected = settings.characterSizeDp == size,
                            onClick = {
                                scope.launch { settingsDataStore.setCharacterSizeDp(size) }
                                onSizeChanged?.invoke(size)
                            },
                            label = { Text(text = "${size}dp") }
                        )
                    }
                }
            }

            OptionSection(title = "台詞頻度") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeechFrequency.entries.forEach { frequency ->
                        FilterChip(
                            selected = settings.speechFrequency == frequency,
                            onClick = {
                                scope.launch { settingsDataStore.setSpeechFrequency(frequency) }
                            },
                            label = { Text(text = frequency.label) }
                        )
                    }
                }
            }

            SettingSwitchRow(
                title = "バッテリーリアクション",
                checked = settings.batteryReactionEnabled,
                onCheckedChange = {
                    scope.launch { settingsDataStore.setBatteryReactionEnabled(it) }
                }
            )
            SettingSwitchRow(
                title = "時間帯リアクション",
                checked = settings.timeReactionEnabled,
                onCheckedChange = {
                    scope.launch { settingsDataStore.setTimeReactionEnabled(it) }
                }
            )
            SettingSwitchRow(
                title = "放置復帰リアクション",
                checked = settings.idleReturnReactionEnabled,
                onCheckedChange = {
                    scope.launch { settingsDataStore.setIdleReturnReactionEnabled(it) }
                }
            )
            SettingSwitchRow(
                title = "深夜リアクション",
                checked = settings.midnightReactionEnabled,
                onCheckedChange = {
                    scope.launch { settingsDataStore.setMidnightReactionEnabled(it) }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = { scope.launch { heroStateDataStore.resetPosition() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "位置リセット")
            }

            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "データ全消去")
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = "データを全消去しますか？") },
            text = { Text(text = "設定と勇者キャラの位置情報を初期状態に戻します。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        scope.launch {
                            settingsDataStore.clear()
                            heroStateDataStore.clear()
                            onDisplayEnabledChanged(true)
                        }
                    }
                ) {
                    Text(text = "消去")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "キャンセル")
                }
            }
        )
    }
}

@Composable
private fun OptionSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
