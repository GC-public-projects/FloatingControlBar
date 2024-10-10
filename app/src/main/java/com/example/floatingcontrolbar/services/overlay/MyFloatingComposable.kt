package com.example.floatingcontrolbar.services.overlay

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.floatingcontrolbar.MainActivity
import com.example.floatingcontrolbar.services.screencapture.MediaProjectionHolder


@Composable
fun MyFloatingComposable(
    hideOverlay: () -> Unit,
    openScreen: (List<Pair<String, String?>>) -> Unit,
    openMediaProjectionPermissionActivity: () -> Unit,
    params: WindowManager.LayoutParams,
    windowManager: WindowManager,
    overlayView: View?
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = Modifier
            .wrapContentSize()
            .background(Color.Red)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y

                    // Update the layout params of the overlayView
                    params.x = offsetX.toInt()
                    params.y = offsetY.toInt()
                    windowManager.updateViewLayout(overlayView, params)
                }
            },
    ) {
        Text(text = "test")
        Button(
            onClick = {
                if (MediaProjectionHolder.mediaProjection == null) {
                    openMediaProjectionPermissionActivity()

                } else {
                    MediaProjectionHolder.screenshotManager?.takeScreenshot(overlayView, openScreen)
                }

            }
        ) {
            Text(text = "Take screenshot & open it")
        }
        Button(
            onClick = {
                openScreen(
                    listOf(
                        MainActivity.INTENT_EXTRA_COMMAND_SHOW_TARGET_SCREEN to
                                "INTENT_EXTRA_TARGET_SCREEN_DATA_MAINSCREEN"
                    )
                )
            },
            modifier = Modifier
                .padding(0.dp)

        ) {
            Text(text = "Open MainScreen")
        }
        Button(
            onClick = {
                openScreen(
                    listOf(
                        MainActivity.INTENT_EXTRA_COMMAND_SHOW_TARGET_SCREEN to
                                "INTENT_EXTRA_TARGET_SCREEN_DATA_SCREEN2"
                    )
                )
            },
            modifier = Modifier
                .padding(0.dp)

        ) {
            Text(text = "Open Screen 2")
        }
        Button(
            onClick = { hideOverlay() },
            modifier = Modifier
                .padding(0.dp)

        ) {
            Text(
                text = "Close Overlay",
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}