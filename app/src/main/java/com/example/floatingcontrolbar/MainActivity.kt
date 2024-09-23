package com.example.floatingcontrolbar

import android.content.Context
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.floatingcontrolbar.screens.MainScreen
import com.example.floatingcontrolbar.screens.Screen2
import com.example.floatingcontrolbar.screens.ScreenScreenshot
import com.example.floatingcontrolbar.ui.theme.FloatingControlBarTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val INTENT_EXTRA_COMMAND_SHOW_TARGET_SCREEN =
            "INTENT_EXTRA_COMMAND_SHOW_TARGET_SCREEN"
        private const val INTENT_EXTRA_TARGET_SCREEN_DATA_MAINSCREEN =
            "INTENT_EXTRA_TARGET_SCREEN_DATA_MAINSCREEN"
        private const val INTENT_EXTRA_TARGET_SCREEN_DATA_SCREEN2 =
            "INTENT_EXTRA_TARGET_SCREEN_DATA_SCREEN2"
        private const val INTENT_EXTRA_TARGET_SCREEN_DATA_SCREENSCREENSHOT =
            "INTENT_EXTRA_TARGET_SCREEN_DATA_SCREENSCREENSHOT"
        private const val INTENT_EXTRA_SCREENSHOT_PATH = "INTENT_EXTRA_SCREENSHOT_PATH"
    }

    private val context: Context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FloatingControlBarTheme {
                var showPermissionDialog by remember { mutableStateOf(false) }
                val modifyShowPermissionDialog = { bool: Boolean -> showPermissionDialog = bool }

                val targetScreenFromService =
                    intent.getStringExtra(INTENT_EXTRA_COMMAND_SHOW_TARGET_SCREEN)

                LaunchedEffect(Unit) {
                    requestNotificationsPermission()
                }

                if (targetScreenFromService != null) {
                    when (targetScreenFromService) {
                        INTENT_EXTRA_TARGET_SCREEN_DATA_MAINSCREEN -> {
                            MainScreen(context, modifyShowPermissionDialog)
                        }

                        INTENT_EXTRA_TARGET_SCREEN_DATA_SCREEN2 -> {
                            Screen2()
                        }

                        INTENT_EXTRA_TARGET_SCREEN_DATA_SCREENSCREENSHOT -> {
                            val screenshotPath = intent.getStringExtra(INTENT_EXTRA_SCREENSHOT_PATH)
                            if (screenshotPath != null) {
                                ScreenScreenshot(screenshotPath)
                            } else {
                                // Handle case where screenshotPath is null
                            }
                        }
                    }
                } else {
                    if (showPermissionDialog) {
                        OverlayPermissionAlertDialog(message = "\"Display over other apps\" permission required !",
                            onDismiss = { modifyShowPermissionDialog(false) },
                            onConfirm = { openOverlaySettings(); modifyShowPermissionDialog(false) })
                    }
                    MainScreen(context, modifyShowPermissionDialog)
                }
            }
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}


