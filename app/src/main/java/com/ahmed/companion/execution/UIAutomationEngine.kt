package com.ahmed.companion.execution

import android.content.Context
import android.util.Log
import com.ahmed.companion.brain.models.ToolCall
import com.ahmed.companion.service.AhmedAccessibilityService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

class UIAutomationEngine(
    private val context: Context,
    private val systemActions: SystemActions
) {

    private val gson = Gson()

    companion object {
        private const val TAG = "UIAutomationEngine"
    }

    suspend fun executeToolCall(toolCall: ToolCall): String {
        val name = toolCall.function.name
        val rawArgs = toolCall.function.arguments

        Log.d(TAG, "Executing tool: $name with args: $rawArgs")

        val args: Map<String, Any> = try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(rawArgs, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val accessibility = AhmedAccessibilityService.instance

        return when (name) {
            "search_youtube" -> {
                val query = args["query"]?.toString() ?: ""
                val shouldPlayFirst = (args["play_first"] as? Boolean) ?: true
                val success = systemActions.searchYouTube(query)
                if (success) {
                    // Allow YouTube to load and render search results
                    delay(2200)

                    if (shouldPlayFirst && accessibility != null) {
                        val clicked = accessibility.clickFirstContentItem()
                        if (clicked) {
                            "بحثت عن $query في يوتيوب وتم تشغيل أول فيديو بنجاح."
                        } else {
                            "بحثت عن $query في يوتيوب والنتائج معروضة الآن."
                        }
                    } else {
                        "بحثت عن: $query في يوتيوب والنتائج معروضة الآن."
                    }
                } else {
                    "حصل خطأ أثناء فتح يوتيوب."
                }
            }

            "click_first_result" -> {
                if (accessibility != null) {
                    val clicked = accessibility.clickFirstContentItem()
                    if (clicked) "تم الضغط على أول فيديو أو نتيجة في الشاشة." else "النتائج ما زالت قيد التحميل."
                } else {
                    "أعد تنشيط صلاحية الـ Accessibility من الإعدادات."
                }
            }

            "send_whatsapp" -> {
                val contact = args["contact_name"]?.toString() ?: ""
                val message = args["message"]?.toString() ?: ""
                if (accessibility != null) {
                    accessibility.automateWhatsAppSend(contact, message)
                    "فتحت الواتس وببحث عن $contact وبكتب الرسالة أوتوماتيك."
                } else {
                    "أعد تنشيط صلاحية الـ Accessibility من الإعدادات."
                }
            }

            "open_app" -> {
                val appName = args["app_name"]?.toString() ?: ""
                val success = systemActions.launchAppByName(appName)
                if (success) {
                    delay(1500) // Allow app to open
                    "فتحت تطبيق $appName بنجاح."
                } else {
                    "ملقتش تطبيق بالاسم ده على الجهاز يا غالي."
                }
            }

            "read_screen" -> {
                if (accessibility != null) {
                    accessibility.dumpScreenState()
                } else {
                    "خدمة إمكانية الوصول غير مفعلة لقراءة الشاشة."
                }
            }

            "click_element" -> {
                val target = args["target"]?.toString() ?: ""
                if (accessibility != null) {
                    val clicked = accessibility.clickElementByText(target)
                    if (clicked) "تم الضغط على $target." else "ملقتش زرار بالاسم ده على الشاشة."
                } else {
                    "خدمة إمكانية الوصول غير مفعلة."
                }
            }

            "type_text" -> {
                val text = args["text"]?.toString() ?: ""
                if (accessibility != null) {
                    val typed = accessibility.typeTextIntoField(text)
                    if (typed) "كتبت: $text." else "ملقتش خانة كتابة نشطة."
                } else {
                    "خدمة إمكانية الوصول غير مفعلة."
                }
            }

            "scroll_screen" -> {
                val direction = args["direction"]?.toString() ?: "down"
                if (accessibility != null) {
                    accessibility.scrollScreen(direction == "down")
                    "تم تمرير الشاشة."
                } else {
                    "خدمة إمكانية الوصول غير مفعلة."
                }
            }

            "set_alarm" -> {
                val hour = (args["hour"] as? Number)?.toInt() ?: 7
                val minute = (args["minute"] as? Number)?.toInt() ?: 0
                val label = args["label"]?.toString() ?: "منبه أحمد"
                val success = systemActions.setAlarm(hour, minute, label)
                if (success) "تم ضبط المنبه على الساعة $hour:$minute بنجاح." else "حصلت مشكلة في ضبط المنبه."
            }

            "device_action" -> {
                val action = args["action"]?.toString() ?: "home"
                if (accessibility != null) {
                    accessibility.performDeviceAction(action)
                    "تم تنفيذ الإجراء: $action."
                } else {
                    "خدمة إمكانية الوصول غير مفعلة."
                }
            }

            else -> "أداة غير معروفة."
        }
    }
}
