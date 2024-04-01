package com.example.voicehelperdemo

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.voicehelperdemo.databinding.ActivityCommanderBinding
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("MissingPermission")
class CommanderActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCommanderBinding.inflate(layoutInflater) }
    private val isRecording = AtomicBoolean(false)
    private val minBufferSize = AudioRecord.getMinBufferSize(
        16000,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioRecord by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }
    private val executor = Executors.newSingleThreadExecutor()
    private val task = Runnable {
        val bytes = ByteArray(minBufferSize)
        while (isRecording.get()) {
            val i = audioRecord.read(bytes, 0, minBufferSize)
            if (i > 0) {
                VoiceHelper.writeDataToSDK(bytes.copyOfRange(0,i),"PCM")
                VoiceHelper.readAbilityResult()
            }
        }
        VoiceHelper.finishCurrentAction()
    }
    private val aiResultListener = object :VoiceHelper.AiResultListener{
        override fun onResult(p0: Int, outputData: AiResponse, p2: Any?) {
            val key: String = outputData.key
            if (key.contains("plain") || key.contains("pgs")){
                runOnUiThread {
                    binding.txt.text = String(outputData.value, Charset.forName("GB2312"))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        VoiceHelper.initSDK(this, resources.getString(R.string.ability_commander))
        VoiceHelper.registerAiResultListener(aiResultListener)
    }

    private fun initView() {
        binding.btn.setOnClickListener {
            if (isRecording.get()) {
                audioRecord.stop()
                isRecording.set(false)
            } else {
                val paramBuilder = AiRequest.builder()
                paramBuilder.param("languageType", 0)
                paramBuilder.param("vadOn", true)
                paramBuilder.param("vadLinkOn", false);
                paramBuilder.param("vadEndGap", 60)
                paramBuilder.param("beamThreshold", 20);
                paramBuilder.param("hisGramThreshold", 3000);
                paramBuilder.param("vadSpeechEnd", 80);
                paramBuilder.param("vadResponsetime", 1000);
                paramBuilder.param("postprocOn", false);
                VoiceHelper.startAction(paramBuilder)
                audioRecord.startRecording()
                isRecording.set(true)
                executor.submit(task)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceHelper.unregisterAiResultListener(aiResultListener)
        VoiceHelper.unInit()
    }
}