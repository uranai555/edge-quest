package com.edgequest.hero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.edgequest.hero.ui.theme.DarkNavy
import com.edgequest.hero.ui.theme.DarkNavyCard
import com.edgequest.hero.ui.theme.Emerald
import com.edgequest.hero.ui.theme.Gold
import com.edgequest.hero.ui.theme.Coral
import com.edgequest.hero.ui.theme.SubText
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.headlineMedium,
                color = Gold,
                fontWeight = FontWeight.Bold
            )

            // セクション：表示
            SectionHeader(title = "表示")

            SettingSwitchRow(
                title = "ユウを表示",
                checked = settings.displayEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        settingsDataStore.setDisplayEnabled(enabled)
                        onDisplayEnabledChanged(enabled)
                    }
                }
            )

            SectionSubtitle(text = "キャラサイズ")
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

            // セクション：ふるまい
            SectionHeader(title = "ふるまい")

            SectionSubtitle(text = "台詞頻度")
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

            SettingSwitchRow(
                title = "深夜リアクション",
                checked = settings.midnightReactionEnabled,
                onCheckedChange = {
                    scope.launch { settingsDataStore.setMidnightReactionEnabled(it) }
                }
            )
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

            // セクション：ユウ
            SectionHeader(title = "ユウ")

            OutlinedButton(
                onClick = { scope.launch { heroStateDataStore.resetPosition() } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "表示位置をリセット")
            }

            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                Text(text = "データ全消去")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // プライバシー
            SectionHeader(title = "プライバシー")
            Text(
                text = "このアプリは通知の中身、画面内容、\n入力内容を読み取りません。\nバッテリー残量と時間帯のみを\nトリガーとして使用します。",
                style = MaterialTheme.typography.bodySmall,
                color = SubText
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = DarkNavyCard,
            titleContentColor = Gold,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = "データを全消去しますか？", color = Gold) },
            text = { Text(text = "設定と勇者キャラの状態を初期状態に戻します。") },
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
                    Text(text = "消去", color = Coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "キャンセル", color = SubText)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Emerald,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SectionSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = SubText
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkNavyCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Emerald,
                checkedTrackColor = Emerald.copy(alpha = 0.3f)
            )
        )
    }
}
