package com.ahmed.companion.brain

import com.ahmed.companion.brain.models.ChatTool
import com.ahmed.companion.brain.models.ToolFunction

object ToolRegistry {

    fun getUniversalTools(): List<ChatTool> {
        return listOf(
            // 1. Dedicated Search on YouTube (Instant Results)
            ChatTool(
                function = ToolFunction(
                    name = "search_youtube",
                    description = "البحث الفوري في يوتيوب وتشغيل النتائج مباشرة لأي سورة أو أغنية أو فيديو أو بودكاست",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "query" to mapOf(
                                "type" to "string",
                                "description" to "عنوان الفيديو أو الكلمات المطلوب البحث عنها في يوتيوب"
                            )
                        ),
                        "required" to listOf("query")
                    )
                )
            ),

            // 2. Send WhatsApp Message (Automated Search & Send)
            ChatTool(
                function = ToolFunction(
                    name = "send_whatsapp",
                    description = "إرسال رسالة واتساب إلى شخص محدد تلقائياً والضغط على زر الإرسال",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "contact_name" to mapOf("type" to "string", "description" to "اسم الشخص أو جهة الاتصال كما هو مسجل في الهاتف"),
                            "message" to mapOf("type" to "string", "description" to "نص الرسالة المطلوب كتابتها وإرسالها")
                        ),
                        "required" to listOf("contact_name", "message")
                    )
                )
            ),

            // 3. Open Any App
            ChatTool(
                function = ToolFunction(
                    name = "open_app",
                    description = "فتح أي تطبيق مثبت على هاتف المستخدم (مثل: يوتيوب، واتساب، المتصفح، المعرض، الإعدادات، تيك توك، فيسبوك، إلخ)",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "app_name" to mapOf(
                                "type" to "string",
                                "description" to "اسم التطبيق بالعربي أو الإنجليزي (مثل: youtube, whatsapp, chrome, gallery)"
                            )
                        ),
                        "required" to listOf("app_name")
                    )
                )
            ),

            // 4. Fast Path: Set Alarm
            ChatTool(
                function = ToolFunction(
                    name = "set_alarm",
                    description = "ضبط منبه النظام مباشرة بتوقيت محدد بدون لمس الشاشة",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "hour" to mapOf("type" to "integer", "description" to "ساعة التنبيه بنظام 24 ساعة (0 إلى 23)"),
                            "minute" to mapOf("type" to "integer", "description" to "دقيقة التنبيه (0 إلى 59)"),
                            "label" to mapOf("type" to "string", "description" to "عنوان أو سبب المنبه")
                        ),
                        "required" to listOf("hour", "minute")
                    )
                )
            ),

            // 5. Read Screen (For autonomous UI steps)
            ChatTool(
                function = ToolFunction(
                    name = "read_screen",
                    description = "قراءة النصوص والأزرار والعناصر المرئية على شاشة الهاتف المفتوحة حالياً لمعرفة ما بداخلها والتفاعل معه",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to emptyMap<String, Any>()
                    )
                )
            ),

            // 6. Click Element on Screen
            ChatTool(
                function = ToolFunction(
                    name = "click_element",
                    description = "الضغط على زر أو عنصر في الشاشة الحالية عبر اسمه أو النص الظاهر عليه أو معرفه",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "target" to mapOf(
                                "type" to "string",
                                "description" to "النص الظاهر على الزر أو معرّف العنصر المطلوب الضغط عليه (مثل: بحث، اشتراك، تشغيل، موافق)"
                            )
                        ),
                        "required" to listOf("target")
                    )
                )
            ),

            // 7. Type Text into Active Input
            ChatTool(
                function = ToolFunction(
                    name = "type_text",
                    description = "كتابة نص في خانة البحث أو حقل الكتابة النشط في التطبيق المفتوح حالياً",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "text" to mapOf(
                                "type" to "string",
                                "description" to "النص المطلوب كتابته والبحث عنه"
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
                    description = "تمرير الشاشة لأعلى أو لأسفل لمشاهدة باقي المحتوى أو الفيديوهات",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "direction" to mapOf(
                                "type" to "string",
                                "enum" to listOf("down", "up"),
                                "description" to "اتجاه التمرير: down للأسفل، up للأعلى"
                            )
                        ),
                        "required" to listOf("direction")
                    )
                )
            ),

            // 9. Device Navigation Actions
            ChatTool(
                function = ToolFunction(
                    name = "device_action",
                    description = "تنفيذ حركة أساسية في الهاتف مثل الرجوع للخلف، الشاشة الرئيسية، أو فتح الإشعارات",
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
