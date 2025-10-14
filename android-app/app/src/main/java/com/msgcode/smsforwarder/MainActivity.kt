package com.msgcode.smsforwarder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var etServerIp: EditText
    private lateinit var etServerPort: EditText
    private lateinit var switchService: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var tvLastMessage: TextView
    private lateinit var tvDebugLog: TextView
    private lateinit var btnTest: Button
    private lateinit var btnSimulateSms: Button
    private lateinit var btnReadSms: Button
    private lateinit var btnSetDefaultSms: Button
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
        btnSimulateSms = findViewById(R.id.btnSimulateSms)
        btnReadSms = findViewById(R.id.btnReadSms)
        btnSetDefaultSms = findViewById(R.id.btnSetDefaultSms)

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

        btnSimulateSms.setOnClickListener {
            simulateSmsReceived()
        }

        btnReadSms.setOnClickListener {
            readRecentSms()
        }

        btnSetDefaultSms.setOnClickListener {
            showHonorSettings()
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
    
    private fun simulateSmsReceived() {
        Log.d("MainActivity", "========== 开始模拟短信测试 ==========")
        
        val serverIp = etServerIp.text.toString().trim()
        val serverPort = etServerPort.text.toString().trim()
        
        // 检查输入
        if (serverIp.isEmpty() || serverPort.isEmpty()) {
            Toast.makeText(this, "❌ 请先填写服务器IP和端口", Toast.LENGTH_LONG).show()
            return
        }
        
        Toast.makeText(this, "🧪 开始模拟短信测试...", Toast.LENGTH_SHORT).show()
        
        // 💡 直接用和测试连接一样的方式！
        Thread {
            try {
                val testMessage = mapOf(
                    "sender" to "10086",
                    "content" to "【测试】您的验证码是123456，请在5分钟内使用。",
                    "timestamp" to System.currentTimeMillis().toString()
                )

                Log.d("MainActivity", "直接调用NetworkHelper.sendSms")
                Log.d("MainActivity", "消息内容: $testMessage")
                
                val success = NetworkHelper.sendSms(serverIp, serverPort, testMessage)

                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@MainActivity, "🎉 模拟短信发送成功！请查看Mac浏览器", Toast.LENGTH_LONG).show()
                        // 更新调试日志
                        prefs.edit().apply {
                            putString("debug_log", "✅ 模拟短信转发成功!\n发件人: 10086\n内容: 【测试】您的验证码是123456")
                            putLong("debug_log_time", System.currentTimeMillis())
                            apply()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "❌ 模拟短信发送失败，请检查Mac服务器", Toast.LENGTH_LONG).show()
                        prefs.edit().apply {
                            putString("debug_log", "❌ 模拟短信转发失败\n目标: $serverIp:$serverPort\n请检查Mac服务器是否运行")
                            putLong("debug_log_time", System.currentTimeMillis())
                            apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "模拟短信测试出错", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ 错误：${e.message}", Toast.LENGTH_LONG).show()
                    prefs.edit().apply {
                        putString("debug_log", "❌ 模拟短信测试异常: ${e.message}")
                        putLong("debug_log_time", System.currentTimeMillis())
                        apply()
                    }
                }
            }
        }.start()
        
        Log.d("MainActivity", "========== 模拟短信测试结束 ==========")
    }
    
    private fun checkTestResult() {
        val debugLog = prefs.getString("debug_log", "")
        Log.d("MainActivity", "检查测试结果 - 调试日志: $debugLog")
        
        when {
            debugLog?.contains("转发成功") == true -> {
                Toast.makeText(this, "🎉 测试成功！转发功能正常工作", Toast.LENGTH_LONG).show()
            }
            debugLog?.contains("转发失败") == true -> {
                Toast.makeText(this, "⚠️ 测试失败：$debugLog", Toast.LENGTH_LONG).show()
            }
            debugLog?.contains("配置缺失") == true -> {
                Toast.makeText(this, "❌ 配置问题：$debugLog", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "⏳ 还在处理中，请稍等...\n当前状态: $debugLog", Toast.LENGTH_LONG).show()
                // 再等3秒检查一次
                Handler(Looper.getMainLooper()).postDelayed({
                    checkTestResult()
                }, 3000)
            }
        }
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
                   append("💡 荣耀手机如收不到短信，点击'权限设置'按钮\n")
                   append("💡 先点击'模拟短信测试'检查转发功能")
               }
           }
        
        tvDebugLog.text = debugInfo
    }
    
    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            packageName == Telephony.Sms.getDefaultSmsPackage(this)
        } else {
            false
        }
    }
    
    private fun showHonorSettings() {
        val message = """
🔧 荣耀手机权限设置指南

⭐ 必须设置 - 电池管理：
设置 → 电池 → 更多电池设置 → 应用启动管理 → 短信转发器
选择"手动管理"：
✅ 允许自启动
✅ 允许关联启动  
✅ 允许后台活动

📱 通知权限：
设置 → 应用和服务 → 应用管理 → 短信转发器
→ 通知 → 允许通知

🛡️ 受保护应用：
手机管家 → 应用启动管理 → 短信转发器 → 设为受保护

💡 完成设置后，重启应用并测试！
        """.trimIndent()
        
        android.app.AlertDialog.Builder(this)
            .setTitle("荣耀手机设置指南")
            .setMessage(message)
            .setPositiveButton("知道了") { dialog, _ ->
                dialog.dismiss()
                // 打开应用设置页面
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "请手动进入设置", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun readRecentSms() {
        Log.d("MainActivity", "========== 开始读取短信 ==========")
        
        // 检查读取短信权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "❌ 需要读取短信权限", Toast.LENGTH_LONG).show()
            return
        }
        
        Toast.makeText(this, "📖 正在读取最近短信...", Toast.LENGTH_SHORT).show()
        
        Thread {
            try {
                val uri = Uri.parse("content://sms")
                val projection = arrayOf("address", "body", "date", "type", "_id")
                val sortOrder = "date DESC"
                
                Log.d("MainActivity", "开始查询短信数据库")
                Log.d("MainActivity", "URI: $uri")
                Log.d("MainActivity", "排序: $sortOrder")
                
                val cursor: Cursor? = contentResolver.query(uri, projection, null, null, sortOrder)
                
                val smsBuilder = StringBuilder()
                smsBuilder.append("📱 最近短信详情:\n\n")
                
                var smsCount = 0
                var totalCount = 0
                var inboxCount = 0
                var outboxCount = 0
                
                cursor?.use {
                    Log.d("MainActivity", "查询成功，cursor总数: ${it.count}")
                    
                    val addressIndex = it.getColumnIndex("address")
                    val bodyIndex = it.getColumnIndex("body") 
                    val dateIndex = it.getColumnIndex("date")
                    val typeIndex = it.getColumnIndex("type")
                    val idIndex = it.getColumnIndex("_id")
                    
                    Log.d("MainActivity", "列索引 - address:$addressIndex, body:$bodyIndex, date:$dateIndex, type:$typeIndex, id:$idIndex")
                    
                    // 先统计所有短信
                    while (it.moveToNext()) {
                        totalCount++
                        val type = if (typeIndex >= 0) it.getInt(typeIndex) else 0
                        val body = if (bodyIndex >= 0) it.getString(bodyIndex) else ""
                        
                        when(type) {
                            1 -> inboxCount++
                            2 -> outboxCount++
                        }
                        
                        Log.d("MainActivity", "短信 $totalCount: type=$type, hasBody=${body.isNotBlank()}, bodyLength=${body.length}")
                        
                        // 只取前10条进行详细显示
                        if (smsCount < 10 && body.isNotBlank()) {
                            smsCount++
                            val address = if (addressIndex >= 0) it.getString(addressIndex) else "未知"
                            val date = if (dateIndex >= 0) it.getLong(dateIndex) else 0L
                            val id = if (idIndex >= 0) it.getLong(idIndex) else 0L
                            
                            val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                            val timeStr = dateFormat.format(Date(date))
                            val now = System.currentTimeMillis()
                            val minutesAgo = (now - date) / (1000 * 60)
                            
                            smsBuilder.append("$smsCount. ID:$id ${when(type) {
                                1 -> "📥收件" 
                                2 -> "📤发件"
                                else -> "📋其他($type)"
                            }}\n")
                            smsBuilder.append("   发件人: $address\n")
                            smsBuilder.append("   时间: $timeStr (${minutesAgo}分钟前)\n")
                            smsBuilder.append("   内容: ${body.take(80)}${if(body.length > 80) "..." else ""}\n\n")
                        }
                    }
                } ?: run {
                    Log.e("MainActivity", "查询返回null cursor")
                }
                
                // 添加统计信息
                smsBuilder.append("📊 统计信息:\n")
                smsBuilder.append("- 总短信数: $totalCount\n")
                smsBuilder.append("- 收件箱: $inboxCount\n")
                smsBuilder.append("- 发件箱: $outboxCount\n")
                smsBuilder.append("- 显示详情: ${smsCount} 条\n\n")
                
                if (totalCount == 0) {
                    smsBuilder.append("❌ 未找到任何短信\n")
                    smsBuilder.append("可能原因:\n")
                    smsBuilder.append("- 权限不足\n") 
                    smsBuilder.append("- 短信数据库为空\n")
                    smsBuilder.append("- 系统限制访问\n")
                } else if (smsCount == 0) {
                    smsBuilder.append("❌ 找到${totalCount}条短信，但都没有内容\n")
                } else {
                    smsBuilder.append("✅ 成功读取并显示 ${smsCount} 条有内容的短信")
                }
                
                val result = smsBuilder.toString()
                Log.d("MainActivity", "短信读取完成:\n$result")
                
                runOnUiThread {
                    // 保存到调试日志
                    prefs.edit().apply {
                        putString("debug_log", result)
                        putLong("debug_log_time", System.currentTimeMillis())
                        apply()
                    }
                    
                    // 显示对话框
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("📖 短信读取结果")
                        .setMessage(result)
                        .setPositiveButton("知道了", null)
                        .setNeutralButton("转发最新短信") { _, _ ->
                            if (inboxCount > 0) {
                                forwardLatestSms()
                            } else {
                                Toast.makeText(this@MainActivity, "❌ 没有收件箱短信可转发", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .show()
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "读取短信失败", e)
                val errorMsg = "❌ 读取短信失败: ${e.message}\n\n详细错误:\n${e.toString()}"
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "读取短信出错: ${e.message}", Toast.LENGTH_LONG).show()
                    
                    prefs.edit().apply {
                        putString("debug_log", errorMsg)
                        putLong("debug_log_time", System.currentTimeMillis())
                        apply()
                    }
                }
            }
        }.start()
        
        Log.d("MainActivity", "========== 短信读取线程已启动 ==========")
    }
    
    private fun forwardLatestSms() {
        Log.d("MainActivity", "========== 开始转发最新短信 ==========")
        
        val serverIp = etServerIp.text.toString().trim()
        val serverPort = etServerPort.text.toString().trim()
        
        if (serverIp.isEmpty() || serverPort.isEmpty()) {
            Toast.makeText(this, "❌ 请先配置服务器地址", Toast.LENGTH_LONG).show()
            return
        }
        
        Thread {
            try {
                val uri = Uri.parse("content://sms")  // 查询所有短信
                val projection = arrayOf("address", "body", "date", "type")
                val selection = "type = 1"  // 只查询收件箱短信
                val sortOrder = "date DESC"
                
                val cursor: Cursor? = contentResolver.query(uri, projection, selection, null, sortOrder)
                
                cursor?.use {
                    // 查找第一条有内容的收件箱短信
                    while (it.moveToNext()) {
                        val addressIndex = it.getColumnIndex("address")
                        val bodyIndex = it.getColumnIndex("body")
                        val dateIndex = it.getColumnIndex("date")
                        val typeIndex = it.getColumnIndex("type")
                        
                        val address = if (addressIndex >= 0) it.getString(addressIndex) else "未知"
                        val body = if (bodyIndex >= 0) it.getString(bodyIndex) else "无内容"
                        val date = if (dateIndex >= 0) it.getLong(dateIndex) else System.currentTimeMillis()
                        val type = if (typeIndex >= 0) it.getInt(typeIndex) else 0
                        
                        // 确保是收件箱短信且有内容
                        if (type == 1 && body.isNotBlank()) {
                            Log.d("MainActivity", "找到最新短信:")
                            Log.d("MainActivity", "  发件人: $address")
                            Log.d("MainActivity", "  内容: $body")
                            Log.d("MainActivity", "  时间: ${Date(date)}")
                            
                            // 💡 直接用和模拟短信测试一样的方式！
                            val smsData = mapOf(
                                "sender" to address,
                                "content" to body,
                                "timestamp" to date.toString()
                            )
                            
                            Log.d("MainActivity", "直接调用NetworkHelper.sendSms转发真实短信")
                            Log.d("MainActivity", "转发数据: $smsData")
                            
                            val success = NetworkHelper.sendSms(serverIp, serverPort, smsData)
                            
                            runOnUiThread {
                                if (success) {
                                    Toast.makeText(this@MainActivity, "🎉 转发成功: $address\n${body.take(30)}...", Toast.LENGTH_LONG).show()
                                    
                                    // 更新调试日志
                                    prefs.edit().apply {
                                        putString("debug_log", "✅ 真实短信转发成功!\n发件人: $address\n内容: ${body.take(50)}${if(body.length > 50) "..." else ""}")
                                        putLong("debug_log_time", System.currentTimeMillis())
                                        apply()
                                    }
                                } else {
                                    Toast.makeText(this@MainActivity, "❌ 转发失败，请检查Mac服务器", Toast.LENGTH_LONG).show()
                                    
                                    prefs.edit().apply {
                                        putString("debug_log", "❌ 真实短信转发失败\n发件人: $address\n目标: $serverIp:$serverPort")
                                        putLong("debug_log_time", System.currentTimeMillis())
                                        apply()
                                    }
                                }
                            }
                            
                            break // 只转发第一条找到的短信
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "转发最新短信失败", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ 转发异常: ${e.message}", Toast.LENGTH_LONG).show()
                    prefs.edit().apply {
                        putString("debug_log", "❌ 转发最新短信异常: ${e.message}")
                        putLong("debug_log_time", System.currentTimeMillis())
                        apply()
                    }
                }
            }
        }.start()
        
        Log.d("MainActivity", "========== 转发最新短信结束 ==========")
    }
}

