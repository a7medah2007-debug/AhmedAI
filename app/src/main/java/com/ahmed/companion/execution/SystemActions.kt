package com.ahmed.companion.execution

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log

class SystemActions(private val context: Context) {

    companion object {
        private const val TAG = "SystemActions"
    }

    /**
     * Set alarm directly via Android Intent
     */
    fun setAlarm(hour: Int, minute: Int, label: String = "منبه أحمد"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm: ${e.message}")
            false
        }
    }

    /**
     * Find and launch any app installed on the device by Arabic or English query
     */
    fun launchAppByName(appNameQuery: String): Boolean {
        val pm = context.packageManager
        val query = appNameQuery.lowercase().trim()

        // Quick common app aliases
        val directPackage = when (query) {
            "youtube", "يوتيوب" -> "com.google.android.youtube"
            "whatsapp", "واتساب", "الواتس" -> "com.whatsapp"
            "chrome", "كروم", "المتصفح" -> "com.android.chrome"
            "facebook", "فيسبوك", "الفيس" -> "com.facebook.katana"
            "instagram", "انستجرام" -> "com.instagram.android"
            "tiktok", "تيك توك" -> "com.zhiliaoapp.musically"
            "telegram", "تيليجرام" -> "org.telegram.messenger"
            "gallery", "المعرض", "الاستوديو" -> "com.google.android.apps.photos"
            "camera", "الكاميرا" -> "com.google.android.GoogleCamera"
            "settings", "الاعدادات", "الضبط" -> "com.android.settings"
            else -> null
        }

        if (directPackage != null) {
            val intent = pm.getLaunchIntentForPackage(directPackage)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            }
        }

        // Search through all installed applications
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in packages) {
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                // Skip plain core system apps unless matched
            }
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Get a short summary of top installed user apps to feed into the System Prompt
     */
    fun getInstalledAppsSummary(): String {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val names = mutableListOf<String>()

            for (appInfo in packages) {
                // Filter to primarily launchable apps
                if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    if (label.isNotBlank() && !label.startsWith("com.")) {
                        names.add(label)
                    }
                }
            }
            names.take(30).joinToString(", ")
        } catch (e: Exception) {
            "يوتيوب، واتساب، كروم، فيسبوك، معرض الصور"
        }
    }
}
