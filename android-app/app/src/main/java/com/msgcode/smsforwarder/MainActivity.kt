package com.msgcode.smsforwarder

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var etServerIp: EditText
    private lateinit var etServerPort: EditText
    private lateinit var switchClipboardSync: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var tvLastCode: TextView
    private lateinit var tvDebugLog: TextView
    private lateinit var btnTest: Button
    private lateinit var btnTestClipboard: Button
    private lateinit var btnViewHistory: Button
    private lateinit var prefs: SharedPreferences
    
    private lateinit var clipboardManager: ClipboardManager
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "ClipboardSyncPrefs"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_CLIPBOARD_ENABLED = "clipboard_enabled"
        
        // 验证码匹配模式
        private val VERIFICATION_CODE_PATTERNS = arrayOf(
            Pattern.compile("验证码[：:\\s]*([0-9]{4,8})"),
            Pattern.compile("验证码为[：:\\s]*([0-9]{4,8})"),
            Pattern.compile("验证码是[：:\\s]*([0-9]{4,8})"),
            Pattern.compile("code[：:\\s]*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9]{4,8})")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initClipboard()
        loadPreferences()
        setupClickListeners()
        startStatusUpdates()
    }

    private fun initViews() {
        etServerIp = findViewById(R.id.etServerIp)
        etServerPort = findViewById(R.id.etServerPort)
        switchClipboardSync = findViewById(R.id.switchService)
        tvStatus = findViewById(R.id.tvStatus)
        tvLastCode = findViewById(R.id.tvLastMessage)
        tvDebugLog = findViewById(R.id.tvDebugLog)
        btnTest = findViewById(R.id.btnTest)
        btnTestClipboard = findViewById(R.id.btnSimulateSms)
        btnViewHistory = findViewById(R.id.btnReadSms)
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun initClipboard() {
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            handleClipboardChange()
        }
    }

    private fun loadPreferences() {
        // 设置默认值
        etServerIp.setText(prefs.getString(KEY_SERVER_IP, "192.168.31.124"))
        etServerPort.setText(prefs.getString(KEY_SERVER_PORT, "5001"))
        
        val clipboardEnabled = prefs.getBoolean(KEY_CLIPBOARD_ENABLED, false)
        switchClipboardSync.isChecked = clipboardEnabled
        
        if (clipboardEnabled) {
            startClipboardMonitoring()
        }
    }

    private fun savePreferences() {
        prefs.edit().apply {
            putString(KEY_SERVER_IP, etServerIp.text.toString().trim())
            putString(KEY_SERVER_PORT, etServerPort.text.toString().trim())
            putBoolean(KEY_CLIPBOARD_ENABLED, switchClipboardSync.isChecked)
            apply()
        }
        Log.d(TAG, "设置已保存 - 剪贴板监听: ${switchClipboardSync.isChecked}")
    }

    private fun setupClickListeners() {
        switchClipboardSync.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startClipboardMonitoring()
                Toast.makeText(this, "✅ 剪贴板监听已启用", Toast.LENGTH_SHORT).show()
            } else {
                stopClipboardMonitoring()
                Toast.makeText(this, "❌ 剪贴板监听已停用", Toast.LENGTH_SHORT).show()
            }
            savePreferences()
        }

        btnTest.setOnClickListener {
            testConnection()
        }

        btnTestClipboard.setOnClickListener {
            testClipboardSync()
        }

        btnViewHistory.setOnClickListener {
            showClipboardHistory()
        }
    }

    private fun startClipboardMonitoring() {
        clipboardListener?.let {
            clipboardManager.addPrimaryClipChangedListener(it)
            Log.d(TAG, "✅ 剪贴板监听已启动")
        }
    }

    private fun stopClipboardMonitoring() {
        clipboardListener?.let {
            clipboardManager.removePrimaryClipChangedListener(it)
            Log.d(TAG, "❌ 剪贴板监听已停止")
        }
    }

    private fun handleClipboardChange() {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString()
                if (!clipText.isNullOrBlank()) {
                    Log.d(TAG, "检测到剪贴板变化: $clipText")
                    
                    val verificationCode = extractVerificationCode(clipText)
                    if (verificationCode != null) {
                        Log.d(TAG, "检测到验证码: $verificationCode")
                        sendClipboardToServer(clipText, verificationCode)
                    } else {
                        Log.d(TAG, "未检测到验证码模式")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理剪贴板变化出错", e)
        }
    }

    private fun extractVerificationCode(text: String): String? {
        for (pattern in VERIFICATION_CODE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val code = if (matcher.groupCount() > 0) {
                    matcher.group(1)
                } else {
                    matcher.group(0)
                }
                // 验证码长度应该在4-8位之间
                if (code != null && code.length in 4..8 && code.all { it.isDigit() }) {
                    return code
                }
            }
        }
        return null
    }

    private fun sendClipboardToServer(content: String, verificationCode: String) {
        val serverIp = etServerIp.text.toString().trim()
        val serverPort = etServerPort.text.toString().trim()
        
        if (serverIp.isEmpty() || serverPort.isEmpty()) {
            Toast.makeText(this, "❌ 请先配置服务器地址", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                val data = mapOf(
                    "content" to content,
                    "verification_code" to verificationCode,
                    "timestamp" to System.currentTimeMillis().toString()
                )

                Log.d(TAG, "发送剪贴板内容到服务器: $data")
                val success = NetworkHelper.sendClipboard(serverIp, serverPort, data)

                runOnUiThread {
                    if (success) {
                        val message = "✅ 验证码已同步: $verificationCode"
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        
                        // 更新界面显示
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        tvLastCode.text = "最近同步：$timeStr\n验证码：$verificationCode"
                        
                        // 保存调试日志
                        val debugMsg = "✅ 验证码同步成功!\n验证码: $verificationCode\n内容: ${content.take(50)}${if(content.length > 50) "..." else ""}"
                        prefs.edit().apply {
                            putString("debug_log", debugMsg)
                            putLong("debug_log_time", System.currentTimeMillis())
                            apply()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "❌ 同步失败，请检查服务器", Toast.LENGTH_LONG).show()
                        prefs.edit().apply {
                            putString("debug_log", "❌ 验证码同步失败\n服务器: $serverIp:$serverPort")
                            putLong("debug_log_time", System.currentTimeMillis())
                            apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送剪贴板内容失败", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ 发送异常: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun testConnection() {
        val serverIp = etServerIp.text.toString().trim()
        val serverPort = etServerPort.text.toString().trim()
        
        if (serverIp.isEmpty() || serverPort.isEmpty()) {
            Toast.makeText(this, "❌ 请先填写服务器地址", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "🔍 测试连接中...", Toast.LENGTH_SHORT).show()
        
        Thread {
            val success = NetworkHelper.testConnection(serverIp, serverPort)
            runOnUiThread {
                if (success) {
                    Toast.makeText(this@MainActivity, "✅ 连接成功！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "❌ 连接失败，请检查IP和端口", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun testClipboardSync() {
        val testContent = "【测试】您的验证码是123456，请在5分钟内使用。"
        val verificationCode = extractVerificationCode(testContent)
        
        if (verificationCode != null) {
            Toast.makeText(this, "🧪 开始测试剪贴板同步...", Toast.LENGTH_SHORT).show()
            sendClipboardToServer(testContent, verificationCode)
        } else {
            Toast.makeText(this, "❌ 测试内容验证码提取失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClipboardHistory() {
        val debugLog = prefs.getString("debug_log", "暂无历史记录")
        val logTime = prefs.getLong("debug_log_time", 0)
        
        val timeStr = if (logTime > 0) {
            SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(logTime))
        } else {
            "未知"
        }
        
        val fullLog = "📋 剪贴板同步历史\n\n最后更新: $timeStr\n\n$debugLog"
        
        android.app.AlertDialog.Builder(this)
            .setTitle("剪贴板同步历史")
            .setMessage(fullLog)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun updateStatus() {
        val clipboardEnabled = switchClipboardSync.isChecked
        val serverIp = etServerIp.text.toString().trim()
        val serverPort = etServerPort.text.toString().trim()
        
        val statusBuilder = StringBuilder()
        statusBuilder.append("🔧 服务配置:\n")
        statusBuilder.append("   IP: ${if(serverIp.isNotEmpty()) serverIp else "未配置"}\n")
        statusBuilder.append("   端口: ${if(serverPort.isNotEmpty()) serverPort else "未配置"}\n\n")
        
        statusBuilder.append("📋 剪贴板监听: ${if(clipboardEnabled) "✅ 已启用" else "❌ 已停用"}\n\n")
        
        statusBuilder.append("💡 使用说明:\n")
        statusBuilder.append("1. 启用剪贴板监听\n")
        statusBuilder.append("2. 收到验证码短信后复制数字\n")
        statusBuilder.append("3. 验证码自动同步到Mac剪贴板\n")
        statusBuilder.append("4. 在Mac上直接粘贴使用")
        
        tvStatus.text = statusBuilder.toString()
        
        // 更新调试日志显示
        val debugLog = prefs.getString("debug_log", "等待剪贴板变化...")
        tvDebugLog.text = "📊 调试信息:\n$debugLog"
    }

    private fun startStatusUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateStatus()
                handler.postDelayed(this, 2000) // 每2秒更新一次
            }
        }
        updateRunnable?.let { handler.post(it) }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        updateRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClipboardMonitoring()
        updateRunnable?.let { handler.removeCallbacks(it) }
    }
}
