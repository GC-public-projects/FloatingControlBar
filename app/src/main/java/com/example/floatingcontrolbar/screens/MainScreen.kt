package com.example.floatingcontrolbar.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.floatingcontrolbar.services.overlay.ComposeOverlayService

@Composable
fun MainScreen(context: Context, modifyShowPermissionDialog: (Boolean) -> Unit) {
    Column {
        Button(onClick = {
            if (!Settings.canDrawOverlays(context)) {
                modifyShowPermissionDialog(true)
            } else {
                ComposeOverlayService.showOverlay(context)
            }
        }) {
            Text(text = "Show Overlay")
        }
        Button(onClick = {
            ComposeOverlayService.hideOverlay(context)
        }) {
            Text(text = "Hide Overlay")
        }
    }
}