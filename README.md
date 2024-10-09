# FloatingControlBar
JetPack compose app whose main purpose is to take a screenshot from a floating bar displayed over all the apps. Once the screenshot is taken, it is displayed an in specific screen. 

The floating bar has some other buttons to browse some screens of the app.

### Project status : Workable, documentation completed

## target audience
This project is for Jetpack Compose initiated user

## Presentation
The goal of this demo is to understand the mechanism to setup in order to be able to capture the screen outside the app (not that simple !)

The buttons of the floating bar that let us browse between some different screens have of main purpose to understand the communication principle between a standelone service and an activity in the 2 directions without using "onBind".

This demo is a follow up of the "FloatingButton" project already in my Github. So the components setup in order to display composables over the apps won't be explained again. 

## Overview
- 1 : Alert dialog notifications
- 2 : Alert AlertDialog open overlay settings
- 3 : Display over the apps settings
- 4 : Floating composable without any activty open
- 5 : Floating composable + screen 2
- 6 : Alert dialog screen capturing
- 7 : Notification of ScreenCaptureService
- 8 : ScreenScreenshot view reduced


<img src="/app/screenshots/screen1.png" alt="Alert dialog notifications" height="390">&emsp;
<img src="/app/screenshots/screen2.png" alt="Alert AlertDialog open overlay settings" height="390">&emsp;
<img src="/app/screenshots/screen3.png" alt="Display over the apps settings" height="390">&emsp;
<img src="/app/screenshots/screen4.png" alt="Floating composable without any activty open" height="390">&emsp;


<img src="/app/screenshots/screen5.png" alt="Floating composable + screen 2" height="390">&emsp;
<img src="/app/screenshots/screen6.png" alt="Alert dialog screen capturing" height="390">&emsp;
<img src="/app/screenshots/screen7.png" alt="Notification of ScreenCaptureService" height="390">&emsp;
<img src="/app/screenshots/screen8.png" alt="ScreenScreenshot view reduced" height="390">&emsp;


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
Obligatory implementation in order to manually stop the screen capture mechanism launched by the Android system from a notification

In AndroidManifest.xml
``` xml
<application
    ... >

    <receiver android:name=".services.screencapture.StopCaptureReceiver" />

    <activity
        ...
```

# Code

# 1. SERVICES/SCREENCAPTURE PACKAGE

## ScreenCaptureService (class)
Foreground service initialized in the Android manifest as such. The onCreate() method calls the required startForeground() function.

### Purpose
Creation of the MediaProjection instance compound of the mandatory elements to capture the screen content regardless the app. The Android system doesn't allow us to create this instance outside a foreground service of type "Media projection" in order to rely with the user's privacy and transparanty policy. Indeed in a foreground service we need to display a persistent notification that will inform the user something is running in the background.

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
Its purpose is to instanciate the ScreenCaptureService.

- `CHANNEL_ID` & `NOTIFICATION_ID` : data mandatory to create the notification and start it
- `EXTRA_RESULT_CODE` & `EXTRA_PROJECTION_DATA` : extra keys constants declaration for the intent creation of the service.


`startServiceWithCallBack(...)` will be called from an activity to start the service. The params of the function are compound of : 

- the activity `context`
- the result code of the  screen capture permission prompt  (-1 : accepted, 0 : denied)
- the `data` generated by the Android system activity in case the permission to capture the screen is accepted. The data is of type `Intent`, So it implements the interface `Parcelable`. That means it can by passed as extra param of an intent. This data is mandatory to create the MediaProjection object.
- A `callback` : the activity that generates the prompt to capture the screen needs to be closed. But if we try to close it directly after instanciated the service, it is to soon and the data generated from this activity is destroyed before the service retrieve it. That's why we created an interface `MediaProjectionCallback` implemented by the activity that starts the service. The interface has just 1 function `onMediaProjectionReady()` that will close the activity in due time. 

So the instance of `MediaProjectionPermissionActivity` is stored as a static component in the companion object and is of type `MediaProjectionCallback`. This instance, as a static one, can be called at any time from any place in the project. in occurence, the method `MediaProjectionReady()` from this instance will be called after the creation of MediaProjection in the `ScreenCaptureService`.

#### MediaProjection 
It is the key component of the service. It will be the root to create all the components mandatory to capture the screen. To ease the access of this instance, it will ba also stored in a singleton `MediaProjectionHolder`

#### onCreate()
To make the service a `Foreground` one it has to be declared in the manifest as such and also call startForeground(...) in this function. The notification as param is created before.


#### onStartCommand(...)
Retrieve the data of the service creation intent in order to create the `Mediaprojection` instance.
Once the instance is created, it is passed to the MediaProjectionHolder singelton. Then an instance of `ScreenshotManager` is created. This User-defined class holds all the components generated thanks to MediaProjection in order to take the screenshot and will be detailed later.

#### onDestroy()
We clean/realease the maximum we can the objects created once the service is stopped.

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
class StopCaptureReceiver : BroadcastReceiver() {
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

- `screenWidth`, `screenHeigh` and `screenDensity` : they are the measurements of the application, in occurence, as the application doesn't have a specific size, the measurement of the entire screen is taken. They are needed to create the imageReader and the virtualDisplay.

- `virtualDisplay` :
container that provides the content of the screen. The images generated by the virtualDisplay are renewed with a frequency that depends of the frame rate of the device. It is instanciated in the init block with the screen characteristics and the imageReader surface

- `imageReader` : Store 1 or many frames of the virtualDisplay in a buffer that can be collected at any time to create an image to store somewhere. As we just need screenshot the buffer size is just 1frame. The image renew frequency depends of the frame rate of the virtualDisplay.


- `mediaProjection` : the key component to generate the virtualDisplay

- `mediaProjectionCallback` : need to be setup and added to the mediaProjection instance. Its purpose is to do some action in case mediaProjection is killed by the user or the system. In occurance its purpose is to release/kill the components generated in order to avoid memory leaks.

- `takeScreenshot(...)` : Public function called from a floatingComposable button to take the screenshot. It takes as param the `overlayView` in order to hide it when the screenshot is taken and the `openScreen` function in order to display the screenshot in a screen. These 2 params belong the `ComposeOverlayService`.

A delay (100) is mandatory to let the time the  overlay view to become invisible before the screenshot is taken as we don't want to see the composeview on the screenshot. I didn't find any other way to avoid the delay, neither by setup a listener for the view changes or by using .isVisible on the view. The issue is the visible status of the view is setup before the view is modified. If someone has a workable way to avoid the setup of a delay to take the screenshot without the overlay view, i will appreciate he let me know...

- `saveBitmapToInternalStorage(...)` : Compress the image in png and save it in the device storage in the app folder.

- `acquireImage(...)` : Private function that retrieves the image from the imageReader buffer, create a bitmap from it, save it to the device and open it in the "ScreenScreenshot" screen from the MainActivity thanks to the 2 extras (1 to target the screen to open and the other one to hold the path of the image stored in the app folder of the device. 

- `release` : Clean up resources when the service is stopped to avoid memory leaks


# 2. SERVICES/OVERLAY PACKAGE

## ComposeOverlayService (class)

### Purpose 
Handles the mechanism to display `MyFloatingComposable` over the apps. See `FloatinButton project` in my github to understand the functions related with the overlay generation.


### Content
Create in package `services` a new package named `overlay`
in this package create Kotlin class named `ComposeOverlayService `

``` kotlin
class ComposeOverlayService : Service(),
    LifecycleOwner,
    SavedStateRegistryOwner {
    companion object {
        private const val INTENT_EXTRA_COMMAND_SHOW_OVERLAY =
            "INTENT_EXTRA_COMMAND_SHOW_OVERLAY"
        private const val INTENT_EXTRA_COMMAND_HIDE_OVERLAY =
            "INTENT_EXTRA_COMMAND_HIDE_OVERLAY"

        private fun startService(
            context: Context,
            command: String,
        ) {
            val intent = Intent(context, ComposeOverlayService::class.java).apply {
                putExtra(command, true)
            }
            context.startService(intent)
        }

        internal fun showOverlay(context: Context) {
            startService(context, INTENT_EXTRA_COMMAND_SHOW_OVERLAY)
        }

        internal fun hideOverlay(context: Context) {
            startService(context, INTENT_EXTRA_COMMAND_HIDE_OVERLAY)
        }
    }

    private val _lifecycleRegistry = LifecycleRegistry(this)
    private val _savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        _savedStateRegistryController.savedStateRegistry
    override val lifecycle: Lifecycle = _lifecycleRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private lateinit var intentOfService: Intent


    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        _savedStateRegistryController.performAttach()
        _savedStateRegistryController.performRestore(null)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw RuntimeException("bound mode not supported")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        intentOfService = intent
        if (intent.hasExtra(INTENT_EXTRA_COMMAND_SHOW_OVERLAY)) {
            showOverlay()
        } else if (intent.hasExtra(INTENT_EXTRA_COMMAND_HIDE_OVERLAY)) {
            hideOverlay()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun showOverlay() {
        if (overlayView != null) return

        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val params = getLayoutParams()

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ComposeOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ComposeOverlayService)
            setContent {
                MyFloatingComposable(
                    ::hideOverlay,
                    ::openScreen,
                    ::openMediaProjectionPermisionActivity,
                    params,
                    windowManager,
                    overlayView
                )
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun hideOverlay() {
        Log.i("MYLOG", "hideOverlay()")
        if (overlayView == null) {
            Log.i("MYLOG", "overlay not shown - aborting")
            return
        }
        windowManager.removeView(overlayView)
        overlayView = null

        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    private fun openScreen(extras: List<Pair<String, String?>>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            // Add all the extras to the intent
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        startActivity(intent)
    }
    private fun openMediaProjectionPermissionActivity() {
        val intent = Intent(this, MediaProjectionPermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        }
        startActivity(intent)
    }

    private fun getLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

    }
}
```

### Component explanations
 2 new functions that don't concern the overlay mechanism have been added in addition of the ones already present in my `FloatinButton project` : 

 - `openScreen(...)` : Function that opens a specific screen in our app. To open a screen, we 1st need to open an activity and from the activity thanks to the extra passed in the intent we can target the screen to open. the param is of type `List\<Pair\<String, String?\>\>` in order to add as many extras we can depending the instructions/data we want to submit.

- `openMediaProjectionPermisionActivity()` : An activity is mandatory to display a permission dialog. In occurence, the permission dialog to record the screen. This activity will be explained on point 3.


## MyFloatingComposable (composable)
4 buttons available ordered in a column. The column has the same modifier method `pointerInput` than the one of the button in the `FloatingButton` project in order to make it draggable

### Content
Create in package `overlay` a new Kotlin file named `MyFloatingComposable`

``` kotlin
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
    Box(
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

        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(text = "test")
            Button(
                onClick = {
                    if(MediaProjectionHolder.mediaProjection == null) {
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
}
```

#### Components explanations
- 1st button : `Take screenshot & open it` : Tests if the MediaProjection instance has been instantiated. If no, `openMediaProjectionPermissionActivity()` is called in order to show the permission dialog for screen capturing, and start the background service `ScreenCaptureService` that will instantiate MediaProjection. If yes, the screenshot is taken. Open screen is passed as param to open the screenshot in the dedicated screen.

- 2nd button `Open MainScreen` : Open the MainScreen with the openScreen function by passsing the dedicated extra to the MainActivity

- 3rd button `Open Screen 2` : Same principle than `Open MainScreen` but to open the Screen 2. The screen 2 content has just a Text that displays "SCREEN 2". Its purpose is just to show how the extras passed to an activity (MainActivity) are used to open different screens.

- 4th button `Close Overlay` : Already present in `FloatinButton project`. Its purpose is to remove the Composeview compound of the floating composable from the WindowManager.


# 3. ACTIVITIES

## MediaProjectionPermissionActivity (class)

### Purpose
Activity implemented in orer to display the permission dialog to capture the screen and start the service `ScreenCaptureService`. It was also possible to use MainActivity but it is better to dedicate another activity for this task in order to not overload the MainActivity and to gain in understanding.

### Content
In the main package create a Kotlin Class file named `MediaProjectionPermissionActivity`

``` kotlin
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
        screenCaptureLauncher =
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
```

### Components explanations
- the interface `MediaProjectionInstantiatedCallback` is created before on the same file as it is very short and linked to the activity.Then the activity implements the interface in order to be used in the `ScreenCaptureService` with only one function available : `onMediaProjectionReady()`

- `mediaProjectionManager` : has got a method `createScreenCaptureIntent()` to generate the intent for the screen capture permission

- `screenCaptureLauncher` : Of type `ActivityResultLauncher\<Intent\>`, the method to initilise it (`registerForActivityResult`) replaces the deprecated overrided method `onActivityResult`. It is setup to launch the screen capture permission request (intent generated by mediaProjectionManager) and handle its result. In this case if the result is `OK`, the intent generated by the permission and the result code are passed as extra params to start the `ScreenCaptureService`. 

Though `onActivityResult` exist but in another place, it is the function implemented by the interface ActivityResultCallback. This interface is the 2nd param of `registerForActivityResult`. Our trailing lambda syntax thanks to the `SAM` conversion is automatically treated as an implementation of the ActivityResultCallback interface, so there's no need to explicitly mention onActivityResult or implement the interface manually. This makes the code cleaner and more concise. In Java, the `SAM` conversion doesn't exists so we can see the override of the method `onActivityResult` when we use `registerForActivityResult`

- `requestScreenCapturePermission()` : method that Calls the method `createScreenCaptureIntent()` from the madiaProjectionManager in order to get the intent of the screen capture permission. Once the intent is affected to `captureIntent`, the method `launch` from screenCaptureLauncher is called with the intent as param.

- `onMediaProjectionReady()` : method that will be called in `ScreenCaptureService` once the MediaProjection instance will have been created. Its purpose is to close the activity in the good time. If no callback is setup, the activity is stopped before the mediaProjection is created.


## MainActivity (class)

### Purposes
- open the different screens of the app following the possible instruction of the extras of the intent setup from the `ComposeOverlayService`
- hold the mechanism to start `ComposeOverlayService`
- handle the overlay permission screen opening by custom permission dialog opening
- handle the Android integrated permission dialog to show notifications (necessary to show the notification of the backround service `ComposeOverlayService`)

### Content
Modify `MainActivity` like that : 
``` kotlin
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
```

### Components explanations
If the MainActivity is normaly started for the 1st time `targetScreenFromService` is null beacause no extra are setup yet. So as `showOverlayPermissionDialog` is first false the `OverlayPermissionAlertDialog` is dislayed and closed once the user went to the settings or refused to go there. Then The MainScreen is open. If the MainActvity is opened from the floating composable, the intent to open it will get some extras in order to target which screen needs to be open.

- `showOverlayPermissionDialog`, `modifyShowOverlayPermissionDialog`, `openOverlaySettings()` : these components are part of the overlay mechanism and already explained in the `FloatingButton` project of my Github


- `companion object` : holds the string constants used as extra of the intent of this MainActivity. 


- `context` : The context of the MainActivity used to call the screen composables and the ComposeOverlayService in the MainScreen

- `onCreate` : retrieve of the intent of the MainActivity in order to get the extra(s) if some exist and open the correct screen in function. The extra key `INTENT_EXTRA_SCREENSHOT_PATH` is used to get the path of the screenshot captured. 

- `requestNotificationsPermission()` : Method that handles the alert dialog generated by the Android System in order to accept the permission for the notifications. When accepted the `ScreenCaptureService` will be able to create the notification that inform the user the service is running.

## OverlayPermissionAlertDialog (Composable)
Same content than the one of the `FloatingButton` project of my Github. So the AlertDialog responsible to open the overlay settings

### Content
In main package create a Kotlin file named `OverlayPermissionAlertDialog`
``` kotlin
@Composable
fun OverlayPermissionAlertDialog(
    message: String, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(text = "Permission Required") },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Overlay Settings")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Deny")
            }
        })
}
```

# 4. SCREENS PACKAGE

## MainScreen (Composable)
Same content than the one of the `FloatingButton` project of my Github. So, 2 buttons taht call the 2 functions of the companion object of the `ComposeOverlayService` in order to start it by displaying or hidding the overay view.

### Content
- create a package named `screens`  in the main package
- in this package, create a kotlin file named MainScreen
``` kotlin
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
```

## Screen2 (Composable)
Just a Text with screen 2 content. The screen 2 purpose is just there to understand the principe of opening a target screen by passing some extra in the intent of an activity.

### Content
In package `screens` create kotlin file named `Screen2`
``` kotlin
@Composable
fun Screen2() {
    Text(
        text = "SCREEN 2",
        fontSize = 50.sp
    )
}
```

## ScreenScreenshot (Composable)

### Purpose
Display the screenshot that was just taken before from the flaoting composable. 

### Content
In package `screens` create kotlin file named ScreenScreenshot
``` kotlin
@Composable
fun ScreenScreenshot(filePath: String?) {
    if (filePath != null) {
        val bitmap = BitmapFactory.decodeFile(filePath)
        bitmap?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop // Crop the image to fill the screen, maintaining the aspect ratio
                )
            }
        }
    }
}
```





