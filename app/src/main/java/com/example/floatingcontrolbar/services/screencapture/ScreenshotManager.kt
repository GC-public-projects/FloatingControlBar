package com.example.floatingcontrolbar.services.screencapture
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.Log
import android.view.View
import com.example.floatingcontrolbar.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScreenshotManager(private val applicationContext: Context, context: Context) {
    private val screenWidth = context.resources.displayMetrics.widthPixels
    private val screenHeight = context.resources.displayMetrics.heightPixels
    private val screenDensity = context.resources.displayMetrics.densityDpi
    private val imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)

    var virtualDisplay: VirtualDisplay? = null
    private val mediaProjection = MediaProjectionHolder.mediaProjection
    private val mediaProjectionCallback: MediaProjection.Callback

    init {
        mediaProjectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                virtualDisplay?.release()
                super.onStop()
            }
        }

        mediaProjection?.registerCallback(mediaProjectionCallback, null)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

    }
    fun takeScreenshot(
        overlayView: View?,
        openScreen: (List<Pair<String, String?>>) -> Unit
    ) : String {
        overlayView?.visibility = View.INVISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            delay(100)

            acquireImage(openScreen)

            overlayView?.visibility = View.VISIBLE
        }

        return ""
    }

    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val file = File(context.filesDir, "$fileName.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    private fun acquireImage(
        openScreen: (List<Pair<String, String?>>)-> Unit
    ) {
        val path: String?
        val image =
            imageReader.acquireLatestImage() // !!!! action to reuse in order to make many screenshots !! > see chatgpt
        if (image != null) {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            image.close()

            path = saveBitmapToInternalStorage(
                applicationContext,
                bitmap,
                "screenshot_${System.currentTimeMillis()}"
            )
            if (path != null) {
                Log.d("Screenshot", "Screenshot saved at $path")
                openScreen(
                    listOf(
                        MainActivity.INTENT_EXTRA_COMMAND_SHOW_TARGET_SCREEN to
                            "INTENT_EXTRA_TARGET_SCREEN_DATA_SCREENSCREENSHOT",
                        "INTENT_EXTRA_SCREENSHOT_PATH" to path)
                )
            } else {
                Log.e("Screenshot", "Failed to save screenshot")
            }
        }
    }

    fun release() {
        // Clean up resources
        mediaProjection?.stop()
        virtualDisplay?.release()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        imageReader.close()
    }
}
