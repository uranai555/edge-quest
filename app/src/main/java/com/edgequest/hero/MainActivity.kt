package com.edgequest.hero

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.edgequest.hero.data.HeroStateDataStore
import com.edgequest.hero.data.SettingsDataStore
import com.edgequest.hero.service.OverlayService
import com.edgequest.hero.ui.PermissionScreen
import com.edgequest.hero.ui.SettingsScreen
import com.edgequest.hero.ui.theme.EdgeQuestTheme

class MainActivity : ComponentActivity() {
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var heroStateDataStore: HeroStateDataStore
    private var canDrawOverlaysState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsDataStore = SettingsDataStore(applicationContext)
        heroStateDataStore = HeroStateDataStore(applicationContext)
        canDrawOverlaysState = canDrawOverlays()

        setContent {
            EdgeQuestTheme {
                LaunchedEffect(canDrawOverlaysState) {
                    if (canDrawOverlaysState) {
                        startOverlayService()
                    }
                }

                if (canDrawOverlaysState) {
                    SettingsScreen(
                        settingsDataStore = settingsDataStore,
                        heroStateDataStore = heroStateDataStore,
                        onDisplayEnabledChanged = { enabled ->
                            if (enabled) startOverlayService() else stopOverlayService()
                        }
                    )
                } else {
                    PermissionScreen(
                        onOpenOverlaySettings = { openOverlayPermissionSettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        canDrawOverlaysState = canDrawOverlays()
        if (::settingsDataStore.isInitialized && canDrawOverlaysState) {
            startOverlayService()
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
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        startService(intent)
    }
}
