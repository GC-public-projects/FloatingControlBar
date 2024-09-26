# FloatingControlBar
JetPack compose app whose main purpose is to take a screenshot of the screen included elements inside and outside the app. The screenshot is triggered from a floating bar that is displayed over all the apps. Once the screenshot is taken, it is displayed an in specific screen. 

The floating bar has some other buttons that let us browse some other screens of our app.

### Project status : Workable, documentation in progress...

## target audience
This project is for Jetpack Compose initiated user

## Presentation
The goal of this demo is to understand the mechanism to setup in order to be able to capture the screen outside the app (not that simple !)

The buttons of the floating bar that let us browse between some different screens have of main purpose to understand the communication principle between a standelone service and an activity in the 2 direction without using "onBind".

This demo is a follow up of the "FloatingButton" project already in my Github. So the components setup in order to display composables over the apps won't be explained again. 

## Overview
<img src="/app/screenshots/screen1.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen2.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen3.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen4.png" alt="" height="390">&emsp;

<img src="/app/screenshots/screen5.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen6.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen7.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen8.png" alt="" height="390">&emsp;


# Init

## Permissions
In AndroidManifest.xml
``` xml
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Services
In AndroidManifest.xml
``` xml
    ...
    </activity>
    <service
        android:name=".services.overlay.ComposeOverlayService"
        android:exported="false"
        android:permission="android.permission.SYSTEM_ALERT_WINDOW" />
    <service
        android:name=".services.screencapture.ScreenCaptureService"
        android:exported="true"
        android:foregroundServiceType="mediaProjection"
        android:permission="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
</application>

```

### Receiver
Obligatory implementation in order to manually stop the screen capture mechanism launched by the Android system

In AndroidManifest.xml
``` xml
<application
    ... >

    <receiver android:name=".services.screencapture.StopCaptureReceiver" />

    <activity
        ...
```

# Code

## ScreenCaptureService (class)
Foreground service initialized in the Android manifest as such. The onCreate() Method calls the required startForeground() function.

### Purpose
Creation of the MediaProjection instance compound of the mandatory elements to capture the screen content regardless the app. The Android sytem doesn't allow us to create this instance outside a foreground service of type "Media projection"in order to rely with the user's privacy and transparanty policy. Indeed in a foreground service we need to display a persistent notification that will inform the user something is running in the background.

### Content
- in the main package, create another package named `services`
- in package `services` create package named `screencapture`
- inside package `screencapture` create kotlin class named `ScreenCaptureService`
``` kotlin
class ScreenCaptureService : Service() {
    companion object {
        const val CHANNEL_ID = "ScreenCaptureChannel"
        const val NOTIFICATION_ID = 1
        private const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        private const val EXTRA_PROJECTION_DATA = "EXTRA_PROJECTION_DATA"

        private var mediaProjectionCallback: MediaProjectionCallback? = null
        fun startServiceWithCallBack(
            context: Context,
            resultCode: Int,
            projectionData: Intent,
            callback: MediaProjectionCallback
        ) {
            mediaProjectionCallback = callback // static instance of MediaProjectionPermissionActivity but onlyonMediaProjectionReady() available
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_PROJECTION_DATA, projectionData)
            }
            context.startForegroundService(intent)
        }
    }

    private var mediaProjection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? {
        throw RuntimeException("bound mode not supported")
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        mediaProjection = null
        MediaProjectionHolder.mediaProjection = null
        MediaProjectionHolder.screenshotManager?.release()
        MediaProjectionHolder.screenshotManager = null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, RESULT_CANCELED)
        val projectionData: Intent? = intent.getParcelableExtra(EXTRA_PROJECTION_DATA)

        if (resultCode == RESULT_OK && projectionData != null) {
            val mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, projectionData)

            MediaProjectionHolder.mediaProjection = mediaProjection
            MediaProjectionHolder.screenshotManager = ScreenshotManager(applicationContext)
            mediaProjectionCallback?.onMediaProjectionReady()
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent =
            Intent(this, StopCaptureReceiver::class.java) // Define a receiver to stop the service
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture")
            .setContentText("Capturing screen...")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Capture",
                stopPendingIntent
            ) // Add stop action
            .build()
    }
}
```

### Components explanations

#### Companion object

Its purpose is to instanciate the ScreenCaptureService. `startServiceWithCallBack(...)` will be called from an activity. The params of the function are compound of : 

- the activity `context`
- the result code of the permission prompt to capture the screen (-1 : accepted, 0 : denied)
- the `data` generated by the Android system activity in case the permission to capture the screen is accepted. The data is of type `Intent`, So it implements the interface `Parcelable`. That means it can by passed as extra param of an intent. This data is mandatory to create the MediaProjection object.
- A `callback` : the activity that generates the prompt to capture the screen needs to be closed. But if we try to close it after instanciated the service, it is to soon and the data generated from this activity is destroyed before it is passed to the service. That's why we created an interface `MediaProjectionCallback` implemented by the activity that starts the service. The interface has just 1 function `onMediaProjectionReady()` that will close the activity in due time. 

So the instance of `MediaProjectionPermissionActivity` is stored as a static component in the companion object and is of type `MediaProjectionCallback`. This instance, as a static one, can be called at any time from any place in the project. in occurence, the method `MediaProjectionReady()` from this instance will be called after the creation of MediaProjection.

#### MediaProjection 
It is the key component of the service. It will be the root to create all the components mandatory to capture the screen. To ease the access of this instance, it will ba also stored in a singleton `MediaProjectionHolder`

#### onCreate()
To make the service a `Foreground` one it has to be declared in the manifest as such and also call startForeground(...) in this function. The notification as param is created before.


#### onStartCommand(...)
Retrieve the data of service creation intent in order to create the `Mediaprojection` instance.
Once the instance is created, it is passed to the MediaProjectionHolder singelton. Then an instance of `ScreenshotManager` is created. This User-defined class has got all the components generated thank to MediaProjection in order to take the screenshot and will be detailed later.

#### onDestroy()
We clean, realease the maximum we can the objects created once the service is stopped.

#### createNotificationChannel()
The channel is mandatory to display a notification.

#### createNotification()
Notification that informs the user the service is running. `stopPendingIntent` holds the intent of a broadcast receiver `StopCaptureReceiver` whose the function is to stop the service. 
The Action of the notification calls stopPendingIntent in order to call the intent of StopCaptureReceiver.


## MediaProjectionHolder (Singleton)

### Purpose
Hold the components generated by `ScreenCaptureService` in order to access them without the need to call `startForegroundService` each time we want to take a screenshot.

### Content
Inside package `screencapture` create kotlin object named `MediaProjectionHolder`
``` kotlin
object MediaProjectionHolder {
    var mediaProjection: MediaProjection? = null
    var screenshotManager: ScreenshotManager? = null
}
```

## StopCaptureReceiver (class)
This BroadcastReceiver also needs to be implemented in the Android manifest > see Init section
Like that it is registered at the system level and can answer to events generated by the notification in occurrence even if the app is not running.


### Purpose
Stop ScreenCaptureService. It is of type `BroadcastReceiver()` in order to be implemented in the PendingIntent of the notification as broadcast.

### Content
Inside package `screencapture` create kotlin class named `StopCaptureReceiver`
``` kotlin
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the ScreenCaptureService if it is running
        val serviceIntent = Intent(context, ScreenCaptureService::class.java)
        context.stopService(serviceIntent)
    }
}
```

## ScreenshotManager (class)

### Purpose
Creates and holds the mechanisms to take screenshots thank to the Mediaprojection Instance

### Content
Inside package `screencapture` create kotlin class named `ScreenshotManager`
``` kotlin
class ScreenshotManager(private val applicationContext: Context) {
    private val screenWidth = applicationContext.resources.displayMetrics.widthPixels
    private val screenHeight = applicationContext.resources.displayMetrics.heightPixels
    private val screenDensity = applicationContext.resources.displayMetrics.densityDpi
    private val imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)

    var virtualDisplay: VirtualDisplay? = null
    private val mediaProjection = MediaProjectionHolder.mediaProjection
    private val mediaProjectionCallback: MediaProjection.Callback

    init {
        mediaProjectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                virtualDisplay?.release()
                MediaProjectionHolder.mediaProjection = null
                imageReader.close()
                val serviceIntent = Intent(applicationContext, ScreenCaptureService::class.java)
                applicationContext.stopService(serviceIntent)
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
```

### Components explanations


