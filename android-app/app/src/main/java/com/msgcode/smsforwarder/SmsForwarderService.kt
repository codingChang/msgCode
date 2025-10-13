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
                serverIp = it.getStringExtra("server_ip") ?: serverIp
                serverPort = it.getStringExtra("server_port") ?: serverPort

                // 保存配置到SharedPreferences
                val prefs = getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("server_ip", serverIp)
                    putString("server_port", serverPort)
                    apply()
                }

                // 如果是转发短信的Action
                if (it.action == "FORWARD_SMS") {
                    val sender = it.getStringExtra("sender") ?: "未知"
                    val content = it.getStringExtra("content") ?: ""
                    val timestamp = it.getLongExtra("timestamp", System.currentTimeMillis())

                    forwardSms(sender, content, timestamp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
        }

        return START_STICKY
    }

    private fun forwardSms(sender: String, content: String, timestamp: Long) {
        Log.d(TAG, "Forwarding SMS from: $sender")

        if (serverIp.isBlank() || serverPort.isBlank()) {
            Log.e(TAG, "Server configuration is missing")
            return
        }

        Thread {
            try {
                val messageData = mapOf(
                    "sender" to sender,
                    "content" to content,
                    "timestamp" to timestamp.toString()
                )

                val success = NetworkHelper.sendSms(serverIp, serverPort, messageData)

                if (success) {
                    Log.d(TAG, "SMS forwarded successfully")
                    updateNotification("✅ 最后转发：$sender")
                } else {
                    Log.e(TAG, "Failed to forward SMS")
                    updateNotification("❌ 转发失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding SMS", e)
                updateNotification("❌ 发送错误")
            }
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}

