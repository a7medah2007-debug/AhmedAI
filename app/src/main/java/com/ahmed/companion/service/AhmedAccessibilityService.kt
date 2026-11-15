package com.ahmed.companion.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo

class AhmedAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AhmedAccessibility"
        var instance: AhmedAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null

        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            for (service in enabledServices) {
                if (service.resolveInfo.serviceInfo.packageName == context.packageName) {
                    return true
                }
            }
            return false
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Ahmed Accessibility Service onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Ahmed Accessibility Service Connected!")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or 
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                     AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Keep active reference fresh
        if (instance == null) {
            instance = this
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Ahmed Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Parse current screen into a clean, structured summary of visible interactive elements
     */
    fun dumpScreenState(): String {
        val rootNode = rootInActiveWindow ?: return "الشاشة فارغة أو جاري تحميل محتواها..."
        val elements = mutableListOf<String>()
        traverseNode(rootNode, elements, 0)
        return if (elements.isEmpty()) {
            "الشاشة قيد التحميل، لا توجد نصوص أو أزرار واضحة حالياً."
        } else {
            elements.take(35).joinToString("\n")
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<String>, depth: Int) {
        if (node == null || depth > 8) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        val label = when {
            !text.isNullOrBlank() -> text
            !desc.isNullOrBlank() -> desc
            else -> null
        }

        if (label != null) {
            val typeStr = when {
                isEditable -> "[حقل كتابة]"
                isClickable -> "[زر]"
                else -> "[نص]"
            }
            list.add("- $typeStr: \"$label\"")
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), list, depth + 1)
        }
    }

    /**
     * Click the first primary video or content card on screen (e.g. YouTube top search result)
     */
    fun clickFirstContentItem(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return findFirstContentNode(rootNode, 0)
    }

    private fun findFirstContentNode(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 8) return false

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Skip top 180px (app bar/search bar) to target actual content
        if (bounds.top > 180 && bounds.height() > 70) {
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""

            if (node.isClickable && (text.isNotBlank() || desc.isNotBlank())) {
                Log.d(TAG, "Clicked first content item: text='$text', desc='$desc'")
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        for (i in 0 until node.childCount) {
            if (findFirstContentNode(node.getChild(i), depth + 1)) {
                return true
            }
        }
        return false
    }

    /**
     * Click on a UI element matching the given text or description
     */
    fun clickElementByText(query: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val cleanQuery = query.lowercase().trim()

        if (cleanQuery == "first_result" || cleanQuery == "first_item" || cleanQuery == "اول فيديو" || cleanQuery == "أول نتيجة") {
            return clickFirstContentItem()
        }

        val matchingNodes = rootNode.findAccessibilityNodeInfosByText(cleanQuery)
        if (matchingNodes.isNotEmpty()) {
            for (node in matchingNodes) {
                if (performClickOnNodeOrParent(node)) {
                    return true
                }
            }
        }

        return deepSearchAndClick(rootNode, cleanQuery)
    }

    private fun deepSearchAndClick(node: AccessibilityNodeInfo?, query: String): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (text.contains(query) || desc.contains(query) || viewId.contains(query)) {
            if (performClickOnNodeOrParent(node)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            if (deepSearchAndClick(node.getChild(i), query)) {
                return true
            }
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    /**
     * Type text into the currently focused or first available editable field
     */
    fun typeTextIntoField(textToType: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        return findAndSetEditable(rootNode, textToType)
    }

    private fun findAndSetEditable(node: AccessibilityNodeInfo?, textToType: String): Boolean {
        if (node == null) return false

        if (node.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        for (i in 0 until node.childCount) {
            if (findAndSetEditable(node.getChild(i), textToType)) {
                return true
            }
        }
        return false
    }

    /**
     * Scroll the screen down or up
     */
    fun scrollScreen(down: Boolean): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val action = if (down) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return rootNode.performAction(action)
    }

    /**
     * Perform global system navigation
     */
    fun performDeviceAction(action: String): Boolean {
        return when (action.lowercase()) {
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "recent_apps" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            else -> false
        }
    }

    /**
     * Multi-step WhatsApp automation flow
     */
    fun automateWhatsAppSend(contactName: String, message: String) {
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(launchIntent)
        }

        handler.postDelayed({
            clickElementByText("Search") || 
            clickElementByText("بحث") || 
            clickElementByText("menuitem_search")

            handler.postDelayed({
                typeTextIntoField(contactName)

                handler.postDelayed({
                    clickElementByText(contactName) || clickFirstContentItem()

                    handler.postDelayed({
                        typeTextIntoField(message)

                        handler.postDelayed({
                            clickElementByText("Send") || 
                            clickElementByText("إرسال") || 
                            clickElementByText("send")
                        }, 700)
                    }, 800)
                }, 1000)
            }, 600)
        }, 1200)
    }
}
