package com.edgequest.hero

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.edgequest.hero.data.HeroStateDataStore
import com.edgequest.hero.data.SettingsDataStore
import com.edgequest.hero.service.OverlayService
import com.edgequest.hero.ui.PermissionScreen
import com.edgequest.hero.ui.SettingsScreen
import com.edgequest.hero.ui.theme.DarkNavy
import com.edgequest.hero.ui.theme.Emerald
import com.edgequest.hero.ui.theme.Gold
import com.edgequest.hero.ui.theme.SubText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var heroStateDataStore: HeroStateDataStore
    private var canDrawOverlaysState by mutableStateOf(false)
    private var showSummonButton by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsDataStore = SettingsDataStore(applicationContext)
        heroStateDataStore = HeroStateDataStore(applicationContext)

        canDrawOverlaysState = canDrawOverlays()

        setContent {
            com.edgequest.hero.ui.theme.EdgeQuestTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkNavy) {
                    if (!canDrawOverlaysState) {
                        PermissionScreen(
                            onOpenOverlaySettings = { openOverlayPermissionSettings() }
                        )
                    } else if (showSummonButton) {
                        SettingsScreen(
                            settingsDataStore = settingsDataStore,
                            heroStateDataStore = heroStateDataStore,
                            onDisplayEnabledChanged = { enabled ->
                                if (enabled) startOverlayService() else stopOverlayService()
                            },
                            onSizeChanged = { size ->
                                val intent = Intent(this@MainActivity, OverlayService::class.java).apply {
                                    action = OverlayService.ACTION_UPDATE_SIZE
                                    putExtra(OverlayService.EXTRA_SIZE_DP, size)
                                }
                                startService(intent)
                            },
                            onDebugAction = { action ->
                                val intent = Intent(this@MainActivity, OverlayService::class.java)
                                if (action.startsWith("stage:")) {
                                    val parts = action.split(":")
                                    if (parts.size >= 2) {
                                        val stage = parts[1].toIntOrNull()
                                        if (stage != null) {
                                            intent.action = OverlayService.ACTION_SET_STAGE
                                            intent.putExtra(OverlayService.EXTRA_STAGE, stage)
                                            startService(intent)
                                        }
                                    }
                                } else if (action.startsWith("trigger:")) {
                                    val parts = action.split(":")
                                    if (parts.size >= 2) {
                                        val type = parts[1]
                                        intent.action = OverlayService.ACTION_TEST_TRIGGER
                                        val triggerType = when (type) {
                                            "低バッテリー" -> "battery"
                                            "深夜" -> "night"
                                            "放置復帰" -> "idle"
                                            "長時間" -> "long"
                                            else -> "tap"
                                        }
                                        intent.putExtra(OverlayService.EXTRA_TRIGGER_TYPE, triggerType)
                                        startService(intent)
                                    }
                                } else if (action == "reset_cooldowns") {
                                    intent.action = OverlayService.ACTION_RESET_COOLDOWNS
                                    startService(intent)
                                }
                            }
                        )
                    } else {
                        SummonScreen(
                            onSummon = {
                                showSummonButton = true
                                startOverlayService()
                            },
                            onOpenSettings = {
                                showSummonButton = true
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = canDrawOverlays()
        if (hasPermission != canDrawOverlaysState) {
            canDrawOverlaysState = hasPermission
        }
    }

    private fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(this)

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra(OverlayService.EXTRA_SIZE_DP, 48)
        }
        CoroutineScope(Dispatchers.Main).launch {
            heroStateDataStore.state.first().let { state ->
                if (state.x != 0 || state.y != 0) {
                    intent.putExtra(OverlayService.EXTRA_POS_X, state.x)
                    intent.putExtra(OverlayService.EXTRA_POS_Y, state.y)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this@MainActivity, intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
private fun SummonScreen(
    onSummon: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "画面端クエスト",
            style = MaterialTheme.typography.headlineLarge,
            color = Gold,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Edge Quest",
            style = MaterialTheme.typography.titleMedium,
            color = SubText
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "スマホの画面端に、小さな勇者を召喚しよう",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSummon,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Emerald,
                contentColor = DarkNavy
            )
        ) {
            Text(
                text = "ユウを召喚する",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(text = "設定を開く")
        }
    }
}