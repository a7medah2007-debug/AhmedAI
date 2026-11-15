package com.ahmed.companion.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ahmed.companion.AhmedApplication
import com.ahmed.companion.MainActivity
import com.ahmed.companion.R
import com.ahmed.companion.brain.DeepSeekClient
import com.ahmed.companion.data.AppPreferences
import com.ahmed.companion.execution.SystemActions
import com.ahmed.companion.execution.UIAutomationEngine
import com.ahmed.companion.voice.SpeechManager
import com.ahmed.companion.voice.TTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AhmedVoiceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var prefs: AppPreferences
    private lateinit var systemActions: SystemActions
    private lateinit var automationEngine: UIAutomationEngine
    private var deepSeekClient: DeepSeekClient? = null

    private var speechManager: SpeechManager? = null
    private var ttsManager: TTSManager? = null

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    companion object {
        private const val TAG = "AhmedVoiceService"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AhmedVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AhmedVoiceService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Ahmed Voice Service Created")

        prefs = AppPreferences(this)
        systemActions = SystemActions(this)
        automationEngine = UIAutomationEngine(this, systemActions)

        if (prefs.hasApiKey) {
            deepSeekClient = DeepSeekClient(prefs.deepSeekApiKey)
        }

        ttsManager = TTSManager(this) { success ->
            Log.d(TAG, "TTS Ready: $success")
        }

        speechManager = SpeechManager(
            context = this,
            onResult = { recognizedText -> handleUserSpeech(recognizedText) },
            onError = { errorMsg -> updateFloatingStatus(errorMsg) },
            onListeningStateChanged = { isListening ->
                updateFloatingStatus(if (isListening) "سامعك يا غالي..." else "أحمد")
            }
        )

        startForeground(NOTIFICATION_ID, buildNotification("أحمد جاهز ومستعد في أي وقت"))
        setupFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (prefs.hasApiKey) {
            deepSeekClient = DeepSeekClient(prefs.deepSeekApiKey)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        speechManager?.destroy()
        ttsManager?.destroy()
        removeFloatingBubble()
        Log.d(TAG, "Ahmed Voice Service Destroyed")
    }

    /**
     * Start voice interaction loop
     */
    fun triggerListening() {
        ttsManager?.stop()
        speechManager?.startListening()
    }

    private fun handleUserSpeech(userSpeech: String) {
        val client = deepSeekClient
        if (client == null) {
            ttsManager?.speak("يا صاحبي، محتاج تحط مفتاح ديب سيك الأول في التطبيق.")
            return
        }

        updateFloatingStatus("بيفكر...")

        serviceScope.launch {
            // Read active screen if available
            val screenState = AhmedAccessibilityService.instance?.dumpScreenState() ?: ""
            val appsSummary = systemActions.getInstalledAppsSummary()

            val result = client.processUserSpeech(userSpeech, screenState, appsSummary)

            when (result) {
                is DeepSeekClient.AgentResult.SpeakAndAct -> {
                    // Speak the response to the user
                    ttsManager?.speak(result.speechResponse)

                    // Execute each tool call sequentially
                    for (toolCall in result.toolCalls) {
                        automationEngine.executeToolCall(toolCall)
                    }
                    updateFloatingStatus("أحمد")
                }

                is DeepSeekClient.AgentResult.SpeakOnly -> {
                    ttsManager?.speak(result.speechResponse)
                    updateFloatingStatus("أحمد")
                }

                is DeepSeekClient.AgentResult.Error -> {
                    ttsManager?.speak(result.errorMessage)
                    updateFloatingStatus("أحمد")
                }
            }
        }
    }

    /**
     * Build Notification for Foreground Service
     */
    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AhmedApplication.CHANNEL_ID)
            .setContentTitle("رفيقك أحمد شغال")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Setup Floating Bubble on Screen
     */
    private fun setupFloatingBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot draw overlay: Permission not granted")
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_bubble, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // Tap to listen & Drag to move
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            triggerListening()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding floating bubble: ${e.message}")
        }
    }

    private fun updateFloatingStatus(text: String) {
        serviceScope.launch(Dispatchers.Main) {
            floatingView?.findViewById<TextView>(R.id.tvBubbleStatus)?.text = text
        }
    }

    private fun removeFloatingBubble() {
        if (floatingView != null && windowManager != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                // Ignore
            }
            floatingView = null
        }
    }
}
