package com.ahmed.companion.brain

import com.ahmed.companion.brain.models.ChatTool
import com.ahmed.companion.brain.models.ToolFunction

object ToolRegistry {

    fun getUniversalTools(): List<ChatTool> {
        return listOf(
            // 1. Dedicated Search on YouTube with Auto-Play
            ChatTool(
                function = ToolFunction(
                    name = "search_youtube",
                    description = "البحث الفوري في يوتيوب وفتح وتشغيل النتائج مباشرة لأي سورة أو فيديو أو محتوى مطلوب",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "query" to mapOf(
                                "type" to "string",
                                "description" to "كلمات البحث بالإنجليزية أو العربية حسب نطق المستخدم الدقيق"
                            ),
                            "play_first" to mapOf(
                                "type" to "boolean",
                                "description" to "تعيين إلى true لتشغيل أول فيديو تلقائياً فور ظهوره"
                            )
                        ),
                        "required" to listOf("query")
                    )
                )
            ),

            // 2. Click First Video or First Result on Screen
            ChatTool(
                function = ToolFunction(
                    name = "click_first_result",
                    description = "الضغط على أول فيديو أو أول نتيجة بحث ظاهرة على الشاشة لتشغيلها فوراً",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to emptyMap<String, Any>()
                    )
                )
            ),

            // 3. Automated WhatsApp Send
            ChatTool(
                function = ToolFunction(
                    name = "send_whatsapp",
                    description = "إرسال رسالة واتساب أوتوماتيكياً (بحث عن الاسم + كتابة الرسالة + ضغط إرسال)",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "contact_name" to mapOf("type" to "string", "description" to "اسم الشخص بالإنجليزية أو العربية كما يطلبه المستخدم"),
                            "message" to mapOf("type" to "string", "description" to "نص الرسالة المطلوب إرسالها")
                        ),
                        "required" to listOf("contact_name", "message")
                    )
                )
            ),

            // 4. Open Any Installed App
            ChatTool(
                function = ToolFunction(
                    name = "open_app",
                    description = "فتح أي تطبيق مثبت على هاتف المستخدم (فيسبوك، تليجرام، إعدادات، متصفح، تيك توك، إلخ)",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "app_name" to mapOf(
                                "type" to "string",
                                "description" to "اسم التطبيق بالعربي أو الإنجليزي"
                            )
                        ),
                        "required" to listOf("app_name")
                    )
                )
            ),

            // 5. Read Screen Elements
            ChatTool(
                function = ToolFunction(
                    name = "read_screen",
                    description = "فحص شاشة الهاتف المفتوحة حالياً لاستخراج النصوص والأزرار لفهم ما تعرضه الشاشة قبل اتخاذ القرار",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to emptyMap<String, Any>()
                    )
                )
            ),

            // 6. Click Element by Text or Description
            ChatTool(
                function = ToolFunction(
                    name = "click_element",
                    description = "الضغط على أي زر أو عنصر مرئي في الشاشة عبر نصه أو معناه",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "target" to mapOf(
                                "type" to "string",
                                "description" to "النص أو المعرف المطلوب النقر عليه"
                            )
                        ),
                        "required" to listOf("target")
                    )
                )
            ),

            // 7. Type Text into Any Active Field
            ChatTool(
                function = ToolFunction(
                    name = "type_text",
                    description = "كتابة نص في خانة البحث أو حقل الإدخال المفتوح على الشاشة",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "text" to mapOf(
                                "type" to "string",
                                "description" to "النص المطلوب كتابته بدقة بنفس اللغة المطلوبة"
                            )
                        ),
                        "required" to listOf("text")
                    )
                )
            ),

            // 8. Scroll Screen
            ChatTool(
                function = ToolFunction(
                    name = "scroll_screen",
                    description = "تمرير الشاشة لأسفل أو لأعلى لاستكشاف المزيد من المحتوى",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "direction" to mapOf(
                                "type" to "string",
                                "enum" to listOf("down", "up"),
                                "description" to "اتجاه التمرير"
                            )
                        ),
                        "required" to listOf("direction")
                    )
                )
            ),

            // 9. Set Alarm
            ChatTool(
                function = ToolFunction(
                    name = "set_alarm",
                    description = "ضبط منبه النظام مباشرة بتوقيت محدد",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "hour" to mapOf("type" to "integer", "description" to "الساعة (0 إلى 23)"),
                            "minute" to mapOf("type" to "integer", "description" to "الدقيقة (0 إلى 59)"),
                            "label" to mapOf("type" to "string", "description" to "سبب المنبه")
                        ),
                        "required" to listOf("hour", "minute")
                    )
                )
            ),

            // 10. Device Action
            ChatTool(
                function = ToolFunction(
                    name = "device_action",
                    description = "تنفيذ حركة أساسية في الهاتف (رجوع back، رئيسية home، إشعارات notifications)",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "action" to mapOf(
                                "type" to "string",
                                "enum" to listOf("home", "back", "notifications", "recent_apps")
                            )
                        ),
                        "required" to listOf("action")
                    )
                )
            )
        )
    }
}
