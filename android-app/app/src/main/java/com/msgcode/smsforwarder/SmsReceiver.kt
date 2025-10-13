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
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // 检查服务是否启用
        val prefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("service_enabled", false)
        
        if (!serviceEnabled) {
            Log.d(TAG, "Service is disabled, ignoring SMS")
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis

                Log.d(TAG, "Received SMS from: $sender")
                Log.d(TAG, "Message: $messageBody")

                // 发送到服务进行转发
                val forwardIntent = Intent(context, SmsForwarderService::class.java).apply {
                    action = "FORWARD_SMS"
                    putExtra("sender", sender)
                    putExtra("content", messageBody)
                    putExtra("timestamp", timestamp)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(forwardIntent)
                } else {
                    context.startService(forwardIntent)
                }

                // 保存最后一条消息信息
                prefs.edit().apply {
                    putString("last_sender", sender)
                    putLong("last_time", timestamp)
                    apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }
}

