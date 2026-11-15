package com.ahmed.companion.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TTSManager(
    context: Context,
    private val onInitComplete: (Boolean) -> Unit
) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "TTSManager"
    }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("ar", "EG")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to general Arabic
                    tts?.setLanguage(Locale("ar"))
                }
                tts?.setSpeechRate(1.05f) // Slightly brisk, lively speaking rate
                tts?.setPitch(1.0f)
                isInitialized = true
                Log.d(TAG, "TTS Initialized successfully")
                onInitComplete(true)
            } else {
                Log.e(TAG, "TTS Initialization failed")
                isInitialized = false
                onInitComplete(false)
            }
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        val utteranceId = "ahmed_speech_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onComplete?.invoke()
            }
            override fun onError(utteranceId: String?) {
                onComplete?.invoke()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
