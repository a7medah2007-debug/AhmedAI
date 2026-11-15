package com.ahmed.companion.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    companion object {
        private const val TAG = "SpeechManager"
    }

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } else {
            Log.e(TAG, "Speech recognition not available on this device")
            onError("التعرف على الصوت غير متاح على جهازك حالياً.")
        }
    }

    fun startListening() {
        if (isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            onListeningStateChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            isListening = false
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        if (!isListening) return
        speechRecognizer?.stopListening()
        isListening = false
        onListeningStateChanged(false)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                onListeningStateChanged(false)
            }

            override fun onError(error: Int) {
                isListening = false
                onListeningStateChanged(false)
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "مسمعتش صوت واضح يا صاحبي."
                    SpeechRecognizer.ERROR_NETWORK -> "مشكلة في اتصال النت أثناء سماع الصوت."
                    SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت من المايك."
                    else -> "حصل خطأ بسيط في التقاط الصوت."
                }
                Log.w(TAG, "SpeechRecognizer Error: $error - $message")
                onError(message)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onListeningStateChanged(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                if (recognizedText.isNotBlank()) {
                    Log.d(TAG, "Speech recognized: $recognizedText")
                    onResult(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
