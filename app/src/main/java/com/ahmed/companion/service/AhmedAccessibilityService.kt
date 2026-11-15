package com.ahmed.companion.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.net.URLEncoder

class AhmedAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AhmedAccessibility"
        var instance: AhmedAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Ahmed Accessibility Service Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events monitored for passive tracking if needed
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
        val rootNode = rootInActiveWindow ?: return "الشاشة فارغة أو غير قابلة للقراءة حالياً."
        val elements = mutableListOf<String>()
        traverseNode(rootNode, elements, 0)
        return if (elements.isEmpty()) {
            "لم يتم العثور على أزرار أو نصوص واضحة في الشاشة الحالية."
        } else {
            elements.take(30).joinToString("\n")
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
                isClickable -> "[زر/عنصر قابل للضغط]"
                else -> "[نص]"
            }
            list.add("- $typeStr: \"$label\"")
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), list, depth + 1)
        }
    }

    /**
     * Click on a UI element matching the given text or description
     */
    fun clickElementByText(query: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val cleanQuery = query.lowercase().trim()

        val matchingNodes = rootNode.findAccessibilityNodeInfosByText(cleanQuery)
        if (matchingNodes.isNotEmpty()) {
            for (node in matchingNodes) {
                if (performClickOnNodeOrParent(node)) {
                    return true
                }
            }
        }

        // Deep fallback search if exact text match isn't immediate
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

        // 1. Try currently focused input
        val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // 2. Otherwise search for any editable field
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
     * Direct WhatsApp dispatch and automated send
     */
    fun sendWhatsAppMessage(contactPhoneOrName: String, message: String): Boolean {
        return try {
            val url = "https://api.whatsapp.com/send?text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)

            // Trigger a delayed auto-click on the send button
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                clickElementByText("Send") || clickElementByText("إرسال") || clickElementByText("send")
            }, 1200)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching WhatsApp: ${e.message}")
            false
        }
    }
}
