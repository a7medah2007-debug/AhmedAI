package com.ahmed.companion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AhmedApplication : Application() {

    companion object {
        const val CHANNEL_ID = "ahmed_companion_channel"
        lateinit var instance: AhmedApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "خدمة الرفيق الذكي أحمد",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تشغيل خدمة الاستماع والمرافقة في الخلفية"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
