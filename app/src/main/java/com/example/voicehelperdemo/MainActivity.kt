package com.example.voicehelperdemo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private val permissions by lazy {
        listOf(
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkPermission()
    }

    private fun checkPermission(){
        val needRequestPermission = mutableListOf<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this,it) != 0) {
                needRequestPermission.add(it)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != 0) {
                needRequestPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {
                needRequestPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + getPackageName())
                startActivityForResult(intent,2)
            }
        }
        if (needRequestPermission.isNotEmpty()){
            ActivityCompat.requestPermissions(this,needRequestPermission.toTypedArray(),100)
        } else {
            initView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 100 || requestCode == 2){
            initView()
        }
    }

    fun initView() {
        findViewById<Button>(R.id.btn_wakeup).setOnClickListener { jumpToWakeUpTask() }
        findViewById<Button>(R.id.btn_dictation).setOnClickListener { jumpToDictationTask() }
        findViewById<Button>(R.id.btn_commander).setOnClickListener { jumpToCommanderTask() }
    }

    private fun jumpToWakeUpTask() {
        val intent = Intent(this,WakeUpActivity::class.java)
        startActivity(intent)
    }

    private fun jumpToDictationTask() {
        val intent = Intent(this,DictationActivity::class.java)
        startActivity(intent)
    }

    private fun jumpToCommanderTask() {
        val intent = Intent(this,CommanderActivity::class.java)
        startActivity(intent)
    }
}