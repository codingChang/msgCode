package com.msgcode.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "========== SmsReceiver.onReceive START ==========")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent extras: ${intent.extras}")
        
        // 保存接收到broadcast的日志
        val prefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        saveDebugLog(prefs, "收到广播: ${intent.action}")
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "Not SMS_RECEIVED_ACTION, ignoring")
            saveDebugLog(prefs, "不是短信广播，已忽略")
            return
        }

        Log.d(TAG, "✅ Received SMS_RECEIVED_ACTION broadcast")

        // 检查服务是否启用
        val serviceEnabled = prefs.getBoolean("service_enabled", false)
        
        Log.d(TAG, "Service enabled status: $serviceEnabled")
        
        if (!serviceEnabled) {
            Log.d(TAG, "❌ Service is disabled, ignoring SMS")
            saveDebugLog(prefs, "服务未启用，已忽略短信")
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d(TAG, "Number of SMS messages: ${messages.size}")
            
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis

                Log.d(TAG, "========================================")
                Log.d(TAG, "📱 SMS Details:")
                Log.d(TAG, "   Sender: $sender")
                Log.d(TAG, "   Content: $messageBody")
                Log.d(TAG, "   Timestamp: $timestamp")
                Log.d(TAG, "========================================")

                // 保存调试信息
                saveDebugLog(prefs, "收到短信\n发件人: $sender\n内容: ${messageBody.take(50)}")

                // 发送到服务进行转发
                val forwardIntent = Intent(context, SmsForwarderService::class.java).apply {
                    action = "FORWARD_SMS"
                    putExtra("sender", sender)
                    putExtra("content", messageBody)
                    putExtra("timestamp", timestamp)
                }

                Log.d(TAG, "🚀 Starting SmsForwarderService to forward SMS")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(forwardIntent)
                } else {
                    context.startService(forwardIntent)
                }

                // 保存最后一条消息信息
                prefs.edit().apply {
                    putString("last_sender", sender)
                    putLong("last_time", timestamp)
                    putString("last_content", messageBody.take(100))
                    apply()
                }
                
                Log.d(TAG, "✅ SMS processing completed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing SMS", e)
            saveDebugLog(prefs, "处理短信出错: ${e.message}")
        }
        
        Log.d(TAG, "========== SmsReceiver.onReceive END ==========")
    }
    
    private fun saveDebugLog(prefs: android.content.SharedPreferences, message: String) {
        try {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val logMessage = "[$timestamp] $message"
            prefs.edit().apply {
                putString("debug_log", logMessage)
                putLong("debug_log_time", System.currentTimeMillis())
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving debug log", e)
        }
    }
}

