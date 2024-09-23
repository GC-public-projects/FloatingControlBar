# FloatingControlBar
JetPack compose app whose main purpose is to is to take a screenshot of the screen included elements inside and outside the app. The screenshot is triggered from a floating bar that is displayed over all the apps. Once the screenshot is taken, it is displayed an in specific screen. 

The floating bar has some other buttons that let us browse some other screens of our app.

### Project status : Workable, documentation in progress...

## target audience
This project is for Jetpack Compose initiated user

## Presentation
The goal of this demo is to understand the mechanism to setup in order to be able to capture the screen outside the app (not that simple !)

The buttons of the floating bar that let us browse between some different screens have of main purpose to understand the communication principle between a standelone service and an activity in the 2 direction without using "onBind".

## Overview
<img src="/app/screenshots/screen1.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen2.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen3.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen4.png" alt="" height="390">&emsp;

<img src="/app/screenshots/screen5.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen6.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen7.png" alt="" height="390">&emsp;
<img src="/app/screenshots/screen8.png" alt="" height="390">&emsp;


## Required


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


