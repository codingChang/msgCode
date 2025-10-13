package com.msgcode.smsforwarder

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var etServerIp: EditText
    private lateinit var etServerPort: EditText
    private lateinit var switchService: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var tvLastMessage: TextView
    private lateinit var tvDebugLog: TextView
    private lateinit var btnTest: Button
    private lateinit var prefs: SharedPreferences
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    companion object {
        private const val PREFS_NAME = "SmsForwarderPrefs"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val REQUEST_CODE_PERMISSIONS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadPreferences()
        checkPermissions()
        updateServiceStatus()
    }

    private fun initViews() {
        etServerIp = findViewById(R.id.etServerIp)
        etServerPort = findViewById(R.id.etServerPort)
        switchService = findViewById(R.id.switchService)
        tvStatus = findViewById(R.id.tvStatus)
        tvLastMessage = findViewById(R.id.tvLastMessage)
        tvDebugLog = findViewById(R.id.tvDebugLog)
        btnTest = findViewById(R.id.btnTest)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkPermissions()) {
                    startForwardingService()
                } else {
                    switchService.isChecked = false
                    Toast.makeText(this, "请先授予必要的权限", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopForwardingService()
            }
        }

        btnTest.setOnClickListener {
            testConnection()
        }
    }

    private fun loadPreferences() {
        etServerIp.setText(prefs.getString(KEY_SERVER_IP, "192.168.31.124"))
        etServerPort.setText(prefs.getString(KEY_SERVER_PORT, "5001"))
        switchService.isChecked = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
    }

    private fun savePreferences() {
        prefs.edit().apply {
            putString(KEY_SERVER_IP, etServerIp.text.toString())
            putString(KEY_SERVER_PORT, etServerPort.text.toString())
            putBoolean(KEY_SERVICE_ENABLED, switchService.isChecked)
            apply()
        }
        Log.d("MainActivity", "Saved preferences: service_enabled=${switchService.isChecked}")
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要所有权限才能正常工作", Toast.LENGTH_LONG).show()
                switchService.isChecked = false
            }
        }
    }

    private fun startForwardingService() {
        val serverIp = etServerIp.text.toString()
        val serverPort = etServerPort.text.toString()

        if (serverIp.isBlank() || serverPort.isBlank()) {
            Toast.makeText(this, "请输入服务器地址和端口", Toast.LENGTH_SHORT).show()
            switchService.isChecked = false
            return
        }

        // 先保存配置
        savePreferences()

        try {
            val intent = Intent(this, SmsForwarderService::class.java).apply {
                putExtra("server_ip", serverIp)
                putExtra("server_port", serverPort)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            updateServiceStatus()
            Toast.makeText(this, "短信转发服务已启动", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动服务失败：${e.message}", Toast.LENGTH_LONG).show()
            switchService.isChecked = false
        }
    }

    private fun stopForwardingService() {
        val intent = Intent(this, SmsForwarderService::class.java)
        stopService(intent)
        savePreferences()
        updateServiceStatus()
        Toast.makeText(this, "短信转发服务已停止", Toast.LENGTH_SHORT).show()
    }

    private fun updateServiceStatus() {
        if (switchService.isChecked) {
            tvStatus.text = "✅ 服务运行中"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvStatus.text = "⭕ 服务已停止"
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun testConnection() {
        val serverIp = etServerIp.text.toString()
        val serverPort = etServerPort.text.toString()

        if (serverIp.isBlank() || serverPort.isBlank()) {
            Toast.makeText(this, "请输入服务器地址和端口", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "正在测试连接...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val testMessage = mapOf(
                    "sender" to "测试",
                    "content" to "这是一条测试消息，时间：${System.currentTimeMillis()}",
                    "timestamp" to System.currentTimeMillis().toString()
                )

                val success = NetworkHelper.sendSms(serverIp, serverPort, testMessage)

                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "✅ 连接成功！", Toast.LENGTH_SHORT).show()
                        tvLastMessage.text = "最后发送：测试消息"
                    } else {
                        Toast.makeText(this, "❌ 连接失败，请检查IP和端口", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "错误：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        updateDebugInfo()
        
        // 启动定时刷新调试信息
        updateRunnable = object : Runnable {
            override fun run() {
                updateDebugInfo()
                handler.postDelayed(this, 1000) // 每秒刷新
            }
        }
        updateRunnable?.let { handler.post(it) }
    }
    
    override fun onPause() {
        super.onPause()
        // 停止定时刷新
        updateRunnable?.let { handler.removeCallbacks(it) }
    }
    
    private fun updateDebugInfo() {
        // 更新最后一条消息显示
        val lastSender = prefs.getString("last_sender", null)
        val lastTime = prefs.getLong("last_time", 0)
        val lastContent = prefs.getString("last_content", null)
        
        if (lastSender != null && lastTime > 0) {
            val timeStr = android.text.format.DateFormat.format("MM-dd HH:mm:ss", lastTime)
            tvLastMessage.text = "最后转发：$lastSender ($timeStr)\n内容：$lastContent"
        }
        
        // 更新调试日志
        val debugLog = prefs.getString("debug_log", null)
        val debugTime = prefs.getLong("debug_log_time", 0)
        val serviceEnabled = prefs.getBoolean("service_enabled", false)
        
        // 检查权限状态
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasReadSmsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        val debugInfo = buildString {
            append("服务状态: ${if (serviceEnabled) "✅ 已启用" else "❌ 未启用"}\n")
            append("接收短信权限: ${if (hasSmsPermission) "✅" else "❌"}\n")
            append("读取短信权限: ${if (hasReadSmsPermission) "✅" else "❌"}\n")
            append("\n")
            if (debugLog != null && debugTime > 0) {
                append("$debugLog")
            } else {
                append("📱 等待短信...\n")
                append("💡 如果收到短信后这里没变化，说明BroadcastReceiver未触发")
            }
        }
        
        tvDebugLog.text = debugInfo
    }
}

