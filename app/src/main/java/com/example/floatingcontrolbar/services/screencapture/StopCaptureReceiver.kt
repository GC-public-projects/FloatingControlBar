package com.example.floatingcontrolbar.services.screencapture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopCaptureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the ScreenCaptureService if it is running
        val serviceIntent = Intent(context, ScreenCaptureService::class.java)
        context.stopService(serviceIntent)
    }
}