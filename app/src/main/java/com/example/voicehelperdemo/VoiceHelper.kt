package com.example.voicehelperdemo

import android.content.Context
import android.util.Log
import com.iflytek.aikit.core.AiAudio
import com.iflytek.aikit.core.AiHandle
import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiListener
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.AiStatus
import com.iflytek.aikit.core.AuthListener
import com.iflytek.aikit.core.BaseLibrary.Params
import com.iflytek.aikit.core.ErrType
import java.util.concurrent.atomic.AtomicReference


object VoiceHelper {
    private const val TAG = "VoiceHelper"
    var currentAction = AtomicReference("")
    val aiHelper: AiHelper = AiHelper.getInst()
    var aiHandle: AiHandle? = null

    private val coreListener = AuthListener { type, code ->
        Log.i(TAG, "core listener code:$code")
        when (type) {
            ErrType.AUTH -> Log.i(TAG, "SDK状态：授权结果码$code")
            ErrType.HTTP -> Log.i(TAG, "SDK状态：HTTP认证结果$code")
            else -> Log.i(TAG, "SDK状态：其他错误")
        }
    }
    private val aiRespListener = object : AiListener {
        override fun onResult(p0: Int, outputData: MutableList<AiResponse>?, p2: Any?) {
            if (null != outputData && outputData.size > 0) {
                for (i in 0 until outputData.size) {
                    val bytes: ByteArray = outputData[i].value ?: continue
                    val key: String = outputData[i].key
                    //获取到结果的key及value，可根据业务保存存储结果或其他处理
                    Log.i(
                        TAG,
                        "onResult: ${"AiOutput{key='" + key + '\'' + "value=${bytes.toString()}" + ", len=" + outputData[i].len + ", type=" + outputData[i].type + ", status=" + outputData[i].status + ", varType=" + outputData[i].varType}"
                    )
                }
            }
        }

        override fun onEvent(p0: Int, p1: Int, p2: MutableList<AiResponse>?, p3: Any?) {
            Log.i(TAG, "onEvent: ")
        }

        override fun onError(p0: Int, p1: Int, p2: String?, p3: Any?) {
            Log.i(TAG, "onError: ")
        }
    }

    fun initSDK(context: Context, ability: String) {
        val params = Params.builder()
            .appId(context.resources.getString(R.string.app_id))
            .apiKey(context.resources.getString(R.string.api_key))
            .apiSecret(context.resources.getString(R.string.api_secret))
            .ability(ability)
            .workDir("/sdcard/iflytekAikit") //SDK工作路径，这里为绝对路径，此处仅为示例
            .build()
        // 初始化
        AiHelper.getInst().registerListener(coreListener);// 注册SDK 初始化状态监听
        AiHelper.getInst().registerListener(
            context.resources.getString(R.string.ability_dictation),
            aiRespListener
        );// 注册能力结果监听
        aiHelper.init(context, params)
        currentAction.set(ability)
    }

    fun startAction(aiRequest: AiRequest.Builder) {
        aiHandle = AiHelper.getInst().start(currentAction.get(), aiRequest.build(), null)
        if (!aiHandle!!.isSuccess) {
            Log.e(TAG, "ERROR::START | handle code:" + aiHandle!!.code)
            return
        }
    }

    fun writeDataToSDK(data: ByteArray, dataType: String) {
        if (aiHandle == null)
            return
        val dataBuilder = AiRequest.builder()
        //输入音频数据
        val PCMData: AiAudio.Holder = AiAudio
            .get(dataType) //输入数据key
            .encoding(AiAudio.ENCODING_DEFAULT) //设置音频类型
            .data(data) //part为 byte[]类型输入数据
        PCMData.status(AiStatus.BEGIN) //送入数据的状态，首帧数据、尾帧数据、中间数据，根据送入数据的状态传入对应的值，取值对应AiStatus.BEGIN、AiStatus.END、AiStatus.CONTINUE

        dataBuilder.payload(PCMData.valid())

        val ret = AiHelper.getInst().write(dataBuilder.build(), aiHandle)
        //ret 值为0 写入成功；非0失败，请参照文档中错误码部分排查
        if (ret != 0) {
            val error = "start write failed$ret"
            Log.e(TAG, error)
        }
    }

    fun readAbilityResult() {
        if (aiHandle == null)
            return
        try {
            val ret = aiHelper.read(currentAction.get(), aiHandle);
            if (ret != 0) {
                val error = "start write failed" + ret;
                Log.e(TAG, error);
            }
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }

    fun finishCurrentAction(){
        if (aiHandle==null)
            return
        val ret = AiHelper.getInst().end(aiHandle)
        if (ret != 0) {
            val error = "end failed$ret"
            Log.e(TAG, error)
        }
    }

    fun unInit() {
        AiHelper.getInst().unInit();
    }
}