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
import com.example.voicehelperdemo.databinding.ActivityDictationBinding
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


@SuppressLint("MissingPermission")
class DictationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDictationBinding.inflate(layoutInflater) }
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
            runOnUiThread {
                binding.txt.text = String(outputData.value, Charset.forName("GB2312"))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        initView()
        VoiceHelper.initSDK(this, resources.getString(R.string.ability_dictation))
        VoiceHelper.registerAiResultListener(aiResultListener)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initView() {
        binding.button.setOnClickListener {
            if (!isRecording.get()) {
                val paramBuilder = AiRequest.builder()
                paramBuilder.param("lmLoad", false);
                paramBuilder.param("vadLoad", false);
                paramBuilder.param("lmOn", false);
                paramBuilder.param("numLoad", false);
                paramBuilder.param("puncLoad", false);
                paramBuilder.param("vadLinkOn", false);
                paramBuilder.param("vadOn", false);
                paramBuilder.param("postprocOn", false);
                paramBuilder.param("vadResponsetime", 6000);
                paramBuilder.param("vadSpeechEnd", 200);
                paramBuilder.param("dialectType", 0);
                paramBuilder.param("htkNeed", true);
                paramBuilder.param("readableNeed", true);
                paramBuilder.param("pgsNeed", true);
                paramBuilder.param("plainNeed", true);
                paramBuilder.param("vadNeed", true);
                VoiceHelper.startAction(paramBuilder)
                audioRecord.startRecording()
                isRecording.set(true)
                executor.submit(task)
            } else {
                audioRecord.stop()
                isRecording.set(false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceHelper.unregisterAiResultListener(aiResultListener)
        VoiceHelper.unInit()
    }
}