package com.ahmed.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ahmed.companion.brain.AhmedPersona
import com.ahmed.companion.brain.DeepSeekClient
import com.ahmed.companion.brain.models.ChatMessage
import com.ahmed.companion.data.AppPreferences
import com.ahmed.companion.databinding.ActivityMainBinding
import com.ahmed.companion.execution.SystemActions
import com.ahmed.companion.execution.UIAutomationEngine
import com.ahmed.companion.service.AhmedAccessibilityService
import com.ahmed.companion.service.AhmedVoiceService
import com.ahmed.companion.voice.SpeechManager
import com.ahmed.companion.voice.TTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPreferences
    private lateinit var systemActions: SystemActions
    private lateinit var automationEngine: UIAutomationEngine

    private var speechManager: SpeechManager? = null
    private var ttsManager: TTSManager? = null

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "تم منح صلاحية الميكروفون بنجاح", Toast.LENGTH_SHORT).show()
            updateUiState()
        } else {
            Toast.makeText(this, "صلاحية الصوت ضرورية لسماع أوامرك", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        systemActions = SystemActions(this)
        automationEngine = UIAutomationEngine(this, systemActions)

        setupUI()
        setupVoiceComponents()

        // Start background companion service
        AhmedVoiceService.start(this)
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun setupUI() {
        binding.etApiKey.setText(prefs.deepSeekApiKey)

        binding.btnSaveApiKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            if (key.isNotEmpty()) {
                prefs.deepSeekApiKey = key
                Toast.makeText(this, "تم حفظ مفتاح DeepSeek بنجاح!", Toast.LENGTH_SHORT).show()
                AhmedVoiceService.start(this)
                updateUiState()
            }
        }

        binding.btnAccessibilityPerm.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "ابحث عن 'خدمة أحمد' وقم بتفعيلها", Toast.LENGTH_LONG).show()
        }

        binding.btnOverlayPerm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        binding.btnMicPerm.setOnClickListener {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        binding.btnTalkToAhmed.setOnClickListener {
            if (!hasAudioPermission()) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }

            binding.tvLiveStatus.text = "سامعك دلوقتي يا غالي..."
            speechManager?.startListening()
        }
    }

    private fun setupVoiceComponents() {
        ttsManager = TTSManager(this) { success ->
            // Ready
        }

        speechManager = SpeechManager(
            context = this,
            onResult = { userSpeech ->
                runAutonomousLoop(userSpeech)
            },
            onError = { errorMsg ->
                binding.tvLiveStatus.text = errorMsg
            },
            onListeningStateChanged = { isListening ->
                binding.tvLiveStatus.text = if (isListening) "سامعك يا غالي... تكلم" else "أحمد جاهز ومستعد"
            }
        )
    }

    /**
     * Strict sequential autonomous loop
     */
    private fun runAutonomousLoop(userSpeech: String) {
        binding.tvLiveStatus.text = "أحمد بيفكر وبيخطط..."
        val client = DeepSeekClient(prefs.deepSeekApiKey)

        CoroutineScope(Dispatchers.Main).launch {
            val appsSummary = systemActions.getInstalledAppsSummary()
            val systemPrompt = AhmedPersona.buildSystemPrompt(appsSummary)

            val conversation = mutableListOf<ChatMessage>()
            conversation.add(ChatMessage(role = "system", content = systemPrompt))

            val initialScreen = AhmedAccessibilityService.instance?.dumpScreenState() ?: ""
            if (initialScreen.isNotBlank()) {
                conversation.add(ChatMessage(role = "system", content = "الشاشة المفتوحة حالياً:\n$initialScreen"))
            }

            conversation.add(ChatMessage(role = "user", content = userSpeech))

            for (step in 1..4) {
                val result = client.executeConversation(conversation)

                when (result) {
                    is DeepSeekClient.AgentResult.SpeakAndAct -> {
                        conversation.add(result.assistantMessage)

                        if (result.speechResponse.isNotBlank()) {
                            binding.tvLiveStatus.text = result.speechResponse
                            ttsManager?.speak(result.speechResponse)
                        }

                        for (tool in result.toolCalls) {
                            val output = automationEngine.executeToolCall(tool)
                            delay(1200)

                            val nextScreen = AhmedAccessibilityService.instance?.dumpScreenState() ?: ""
                            val combinedOutput = "$output\nالشاشة الآن تعرض:\n$nextScreen"

                            conversation.add(
                                ChatMessage(
                                    role = "tool",
                                    toolCallId = tool.id,
                                    content = combinedOutput
                                )
                            )
                        }
                    }

                    is DeepSeekClient.AgentResult.SpeakOnly -> {
                        binding.tvLiveStatus.text = result.speechResponse
                        ttsManager?.speak(result.speechResponse)
                        break
                    }

                    is DeepSeekClient.AgentResult.Error -> {
                        binding.tvLiveStatus.text = result.errorMessage
                        ttsManager?.speak(result.errorMessage)
                        break
                    }
                }
            }
        }
    }

    private fun updateUiState() {
        val isAccessibilityEnabled = AhmedAccessibilityService.isServiceRunning
        if (isAccessibilityEnabled) {
            binding.btnAccessibilityPerm.text = "مفعلة ✓"
            binding.btnAccessibilityPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            binding.btnAccessibilityPerm.text = "تفعيل الصلاحية"
            binding.btnAccessibilityPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                binding.btnOverlayPerm.text = "مفعلة ✓"
                binding.btnOverlayPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_green))
            } else {
                binding.btnOverlayPerm.text = "تفعيل الزر العائم"
                binding.btnOverlayPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            }
        }

        if (hasAudioPermission()) {
            binding.btnMicPerm.text = "ممنوحة ✓"
            binding.btnMicPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            binding.btnMicPerm.text = "منح إذن الصوت"
            binding.btnMicPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager?.destroy()
        ttsManager?.destroy()
    }
}
