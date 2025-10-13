package com.msgcode.smsforwarder

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class SmsForwarderService : Service() {

    companion object {
        private const val TAG = "SmsForwarderService"
        private const val CHANNEL_ID = "SmsForwarderChannel"
        private const val NOTIFICATION_ID = 1
    }

    private var serverIp: String = ""
    private var serverPort: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        try {
            // 启动前台服务（必须在5秒内调用）
            val notification = createNotification("短信转发服务运行中")
            startForeground(NOTIFICATION_ID, notification)

            // 从Intent获取服务器配置
            intent?.let {
                // 如果是转发短信的Action，直接转发
                if (it.action == "FORWARD_SMS") {
                    // 从SharedPreferences读取配置
                    val prefs = getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
                    serverIp = prefs.getString("server_ip", "") ?: ""
                    serverPort = prefs.getString("server_port", "") ?: ""
                    
                    val sender = it.getStringExtra("sender") ?: "未知"
                    val content = it.getStringExtra("content") ?: ""
                    val timestamp = it.getLongExtra("timestamp", System.currentTimeMillis())

                    Log.d(TAG, "Forwarding SMS - ServerIP: $serverIp, Port: $serverPort")
                    forwardSms(sender, content, timestamp)
                } else {
                    // 正常启动服务，保存配置
                    serverIp = it.getStringExtra("server_ip") ?: serverIp
                    serverPort = it.getStringExtra("server_port") ?: serverPort

                    // 保存配置到SharedPreferences
                    val prefs = getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("server_ip", serverIp)
                        putString("server_port", serverPort)
                        apply()
                    }
                    Log.d(TAG, "Service started with IP: $serverIp, Port: $serverPort")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
        }

        return START_STICKY
    }

    private fun forwardSms(sender: String, content: String, timestamp: Long) {
        Log.d(TAG, "========== SmsForwarderService开始转发 ==========")
        Log.d(TAG, "发件人: $sender")
        Log.d(TAG, "内容: $content")
        Log.d(TAG, "时间戳: $timestamp")
        Log.d(TAG, "服务器: $serverIp:$serverPort")

        if (serverIp.isBlank() || serverPort.isBlank()) {
            Log.e(TAG, "❌ 服务器配置缺失: IP='$serverIp', Port='$serverPort'")
            saveDebugLog("❌ 转发失败: 服务器配置缺失\nIP: '$serverIp'\nPort: '$serverPort'")
            updateNotification("❌ 配置错误")
            return
        }

        Thread {
            try {
                updateNotification("📤 转发中...")
                
                val messageData = mapOf(
                    "sender" to sender,
                    "content" to content,
                    "timestamp" to timestamp.toString()
                )

                Log.d(TAG, "调用NetworkHelper.sendSms")
                val success = NetworkHelper.sendSms(serverIp, serverPort, messageData)

                if (success) {
                    Log.d(TAG, "✅ SMS转发成功!")
                    val successMsg = "✅ 转发成功!\n发件人: $sender\n内容: ${content.take(50)}${if(content.length > 50) "..." else ""}"
                    saveDebugLog(successMsg)
                    updateNotification("✅ 转发成功: $sender")
                } else {
                    Log.e(TAG, "❌ SMS转发失败")
                    val failMsg = "❌ 转发失败\n发件人: $sender\n可能原因:\n- Mac服务器未运行\n- 网络连接问题\n- 服务器地址错误"
                    saveDebugLog(failMsg)
                    updateNotification("❌ 转发失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 转发过程出错", e)
                val errorMsg = "❌ 转发异常: ${e.message}\n发件人: $sender\n服务器: $serverIp:$serverPort"
                saveDebugLog(errorMsg)
                updateNotification("❌ 转发异常")
            }
            Log.d(TAG, "========== SmsForwarderService转发结束 ==========")
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "短信转发服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持短信转发服务运行"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("短信转发器")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun saveDebugLog(message: String) {
        try {
            val prefs = getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("debug_log", message)
                putLong("debug_log_time", System.currentTimeMillis())
                apply()
            }
            Log.d(TAG, "调试日志已保存: $message")
        } catch (e: Exception) {
            Log.e(TAG, "保存调试日志失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}

