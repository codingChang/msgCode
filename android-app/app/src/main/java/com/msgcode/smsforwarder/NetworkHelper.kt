package com.msgcode.smsforwarder

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object NetworkHelper {
    
    private const val TAG = "NetworkHelper"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * 发送短信到Mac服务器
     */
    fun sendSms(serverIp: String, serverPort: String, messageData: Map<String, String>): Boolean {
        return try {
            val url = "http://$serverIp:$serverPort/api/sms"
            val json = gson.toJson(messageData)
            
            Log.d(TAG, "Sending SMS to: $url")
            Log.d(TAG, "Data: $json")
            
            val body = json.toRequestBody(JSON)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: ${response.body?.string()}")
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
            false
        }
    }

    /**
     * 测试连接
     */
    fun testConnection(serverIp: String, serverPort: String): Boolean {
        return try {
            val url = "http://$serverIp:$serverPort/api/messages"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing connection", e)
            false
        }
    }
}

