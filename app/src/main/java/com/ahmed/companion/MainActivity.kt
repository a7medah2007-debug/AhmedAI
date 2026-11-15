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
import com.ahmed.companion.brain.DeepSeekClient
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

        // Automatically start the background service if permissions are ready
        if (prefs.hasApiKey) {
            AhmedVoiceService.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun setupUI() {
        // Load API Key
        binding.etApiKey.setText(prefs.deepSeekApiKey)

        // Save API Key
        binding.btnSaveApiKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            if (key.isNotEmpty()) {
                prefs.deepSeekApiKey = key
                Toast.makeText(this, "تم حفظ مفتاح DeepSeek بنجاح!", Toast.LENGTH_SHORT).show()
                AhmedVoiceService.start(this)
                updateUiState()
            } else {
                Toast.makeText(this, "يرجى إدخال مفتاح صالح", Toast.LENGTH_SHORT).show()
            }
        }

        // Accessibility Permission Button
        binding.btnAccessibilityPerm.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "ابحث عن 'خدمة أحمد' وقم بتفعيلها", Toast.LENGTH_LONG).show()
        }

        // Overlay Permission Button
        binding.btnOverlayPerm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // Mic Permission Button
        binding.btnMicPerm.setOnClickListener {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Direct Voice Button
        binding.btnTalkToAhmed.setOnClickListener {
            if (!hasAudioPermission()) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }

            if (!prefs.hasApiKey) {
                Toast.makeText(this, "احفظ مفتاح DeepSeek أولاً يا غالي", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            binding.tvLiveStatus.text = "سامعك دلوقتي يا غالي..."
            speechManager?.startListening()
        }
    }

    private fun setupVoiceComponents() {
        ttsManager = TTSManager(this) { success ->
            if (success) {
                // Warm up
            }
        }

        speechManager = SpeechManager(
            context = this,
            onResult = { userSpeech ->
                processDirectUserSpeech(userSpeech)
            },
            onError = { errorMsg ->
                binding.tvLiveStatus.text = errorMsg
            },
            onListeningStateChanged = { isListening ->
                binding.tvLiveStatus.text = if (isListening) "سامعك يا غالي... تكلم" else "أحمد جاهز ومستعد"
            }
        )
    }

    private fun processDirectUserSpeech(userSpeech: String) {
        binding.tvLiveStatus.text = "أحمد بيفكر وبيخطط..."

        val client = DeepSeekClient(prefs.deepSeekApiKey)
        CoroutineScope(Dispatchers.Main).launch {
            val screenState = AhmedAccessibilityService.instance?.dumpScreenState() ?: ""
            val appsSummary = systemActions.getInstalledAppsSummary()

            val result = client.processUserSpeech(userSpeech, screenState, appsSummary)

            when (result) {
                is DeepSeekClient.AgentResult.SpeakAndAct -> {
                    binding.tvLiveStatus.text = result.speechResponse
                    ttsManager?.speak(result.speechResponse)

                    for (tool in result.toolCalls) {
                        automationEngine.executeToolCall(tool)
                    }
                }

                is DeepSeekClient.AgentResult.SpeakOnly -> {
                    binding.tvLiveStatus.text = result.speechResponse
                    ttsManager?.speak(result.speechResponse)
                }

                is DeepSeekClient.AgentResult.Error -> {
                    binding.tvLiveStatus.text = result.errorMessage
                    ttsManager?.speak(result.errorMessage)
                }
            }
        }
    }

    private fun updateUiState() {
        // Accessibility state
        val isAccessibilityEnabled = AhmedAccessibilityService.isServiceRunning
        if (isAccessibilityEnabled) {
            binding.btnAccessibilityPerm.text = "مفعلة ✓"
            binding.btnAccessibilityPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            binding.btnAccessibilityPerm.text = "تفعيل الصلاحية"
        }

        // Overlay state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                binding.btnOverlayPerm.text = "مفعلة ✓"
                binding.btnOverlayPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_green))
            } else {
                binding.btnOverlayPerm.text = "تفعيل الزر العائم"
            }
        }

        // Mic state
        if (hasAudioPermission()) {
            binding.btnMicPerm.text = "ممنوحة ✓"
            binding.btnMicPerm.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            binding.btnMicPerm.text = "منح إذن الصوت"
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
