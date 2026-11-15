package com.ahmed.companion.brain

import android.util.Log
import com.ahmed.companion.brain.models.ChatCompletionRequest
import com.ahmed.companion.brain.models.ChatMessage
import com.ahmed.companion.brain.models.ChatResponse
import com.ahmed.companion.brain.models.ToolCall
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DeepSeekClient(private val apiKey: String) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "DeepSeekClient"
        private const val API_URL = "https://api.deepseek.com/chat/completions"
    }

    sealed class AgentResult {
        data class SpeakAndAct(
            val speechResponse: String,
            val toolCalls: List<ToolCall>
        ) : AgentResult()

        data class SpeakOnly(val speechResponse: String) : AgentResult()
        data class Error(val errorMessage: String) : AgentResult()
    }

    suspend fun executeConversation(messages: List<ChatMessage>): AgentResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AgentResult.Error("مفتاح DeepSeek غير متوفر.")
        }

        try {
            val requestBodyObj = ChatCompletionRequest(
                model = "deepseek-chat",
                messages = messages,
                tools = ToolRegistry.getUniversalTools(),
                toolChoice = "auto",
                temperature = 0.2
            )

            val jsonBody = gson.toJson(requestBodyObj)
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "API Error: code=${response.code}, body=$responseBody")
                return@withContext AgentResult.Error("مشكلة في الاتصال بسيرفر الذكاء الاصطناعي.")
            }

            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            val choice = chatResponse.choices?.firstOrNull()
            val message = choice?.message

            val contentText = message?.content?.trim() ?: ""
            val toolCalls = message?.toolCalls ?: emptyList()

            if (toolCalls.isNotEmpty()) {
                AgentResult.SpeakAndAct(contentText, toolCalls)
            } else if (contentText.isNotBlank()) {
                AgentResult.SpeakOnly(contentText)
            } else {
                AgentResult.SpeakOnly("تم يا غالي!")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception in executeConversation: ${e.message}", e)
            AgentResult.Error("حصل خطأ بسيط في الاتصال، جرب تاني.")
        }
    }

    suspend fun processUserSpeech(
        userText: String,
        screenContext: String = "",
        installedAppsSummary: String = ""
    ): AgentResult {
        val systemPrompt = AhmedPersona.buildSystemPrompt(installedAppsSummary)
        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage(role = "system", content = systemPrompt))

        if (screenContext.isNotBlank()) {
            messages.add(
                ChatMessage(
                    role = "system",
                    content = "الشاشة المفتوحة حالياً:\n$screenContext"
                )
            )
        }

        messages.add(ChatMessage(role = "user", content = userText))
        return executeConversation(messages)
    }
}
