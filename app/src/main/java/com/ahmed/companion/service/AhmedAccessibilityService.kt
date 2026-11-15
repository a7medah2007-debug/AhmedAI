package com.ahmed.companion.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Ahmed Accessibility Service Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event stream
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
     * High-Reliability Full WhatsApp Automation Flow:
     * 1. Open WhatsApp
     * 2. Click Search
     * 3. Type Contact Name
     * 4. Open Contact Chat
     * 5. Type Message
     * 6. Click Send
     */
    fun automateWhatsAppSend(contactName: String, message: String) {
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(launchIntent)
        }

        // Step 1: Click Search icon after WhatsApp opens
        handler.postDelayed({
            val clickedSearch = clickElementByText("Search") || 
                                clickElementByText("بحث") || 
                                clickElementByText("menuitem_search")
            Log.d(TAG, "Clicked WhatsApp Search: $clickedSearch")

            // Step 2: Type contact name into search field
            handler.postDelayed({
                typeTextIntoField(contactName)
                Log.d(TAG, "Typed contact name: $contactName")

                // Step 3: Click contact name in search results
                handler.postDelayed({
                    val clickedContact = clickElementByText(contactName)
                    Log.d(TAG, "Clicked contact: $clickedContact")

                    // Step 4: Type the message into the message input
                    handler.postDelayed({
                        typeTextIntoField(message)
                        Log.d(TAG, "Typed message: $message")

                        // Step 5: Click Send button
                        handler.postDelayed({
                            val clickedSend = clickElementByText("Send") || 
                                              clickElementByText("إرسال") || 
                                              clickElementByText("send")
                            Log.d(TAG, "Clicked send button: $clickedSend")
                        }, 700)
                    }, 800)
                }, 1000)
            }, 600)
        }, 1200)
    }
}
