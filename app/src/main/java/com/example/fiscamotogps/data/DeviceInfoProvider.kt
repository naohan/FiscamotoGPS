package com.example.fiscamotogps.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.fiscamotogps.data.remote.model.DeviceInfoPayload

class DeviceInfoProvider(private val context: Context) {

    fun createPayload(): DeviceInfoPayload {
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val version = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()

        return DeviceInfoPayload(
            deviceId = deviceId,
            platform = "android",
            model = model,
            version = version
        )
    }
}


