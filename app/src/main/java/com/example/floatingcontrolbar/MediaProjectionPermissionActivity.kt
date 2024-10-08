package com.example.floatingcontrolbar

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.floatingcontrolbar.services.screencapture.MediaProjectionHolder
import com.example.floatingcontrolbar.services.screencapture.ScreenCaptureService

interface MediaProjectionInstantiatedCallback {
    fun onMediaProjectionReady()
}
class MediaProjectionPermissionActivity : ComponentActivity(), MediaProjectionInstantiatedCallback{
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher = // params are ActivityResultContract &
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    ScreenCaptureService.startServiceWithCallBack(this, result.resultCode, result.data!!, this)
                } else {
                    // handle the case if permission not granted or no intent generated
                }
            }
        setContent {
            LaunchedEffect(Unit) { requestScreenCapturePermission() }
        }
    }

    private fun requestScreenCapturePermission() {
        if (MediaProjectionHolder.mediaProjection == null) {
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            screenCaptureLauncher.launch(captureIntent)
        }
    }

    override fun onMediaProjectionReady() {
        finish()
    }
}