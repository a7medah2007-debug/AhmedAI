package com.ahmed.companion.brain.models

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model") val model: String = "deepseek-chat",
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("tools") val tools: List<ChatTool>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = "auto",
    @SerializedName("temperature") val temperature: Double = 0.4,
    @SerializedName("max_tokens") val maxTokens: Int = 1000
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
)

data class ChatTool(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: ToolFunction
)

data class ToolFunction(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: Map<String, Any>
)

data class ChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<ChatChoice>?
)

data class ChatChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatMessage
)

data class ToolCall(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("function") val function: ToolCallFunction
)

data class ToolCallFunction(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: String
)
