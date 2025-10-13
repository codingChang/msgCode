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
            
            // 转换timestamp为数字
            val processedData = messageData.toMutableMap()
            processedData["timestamp"]?.let { timestamp ->
                try {
                    processedData["timestamp"] = timestamp.toLong().toString()
                } catch (e: Exception) {
                    Log.w(TAG, "无法转换timestamp: $timestamp")
                }
            }
            
            val json = gson.toJson(processedData)
            
            Log.d(TAG, "========== NetworkHelper发送SMS ==========")
            Log.d(TAG, "目标URL: $url")
            Log.d(TAG, "发送数据: $json")
            
            val body = json.toRequestBody(JSON)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                val success = response.isSuccessful
                
                Log.d(TAG, "响应状态码: ${response.code}")
                Log.d(TAG, "响应内容: $responseBody")
                Log.d(TAG, "发送${if(success) "成功" else "失败"}")
                Log.d(TAG, "========== NetworkHelper结束 ==========")
                
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ NetworkHelper发送SMS出错", e)
            Log.e(TAG, "错误详情: ${e.message}")
            Log.e(TAG, "服务器: $serverIp:$serverPort")
            false
        }
    }

    /**
     * 测试连接
     */
    fun testConnection(serverIp: String, serverPort: String): Boolean {
        return try {
            val url = "http://$serverIp:$serverPort/api/messages"
            
            Log.d(TAG, "Testing connection to: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Test connection response: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing connection", e)
            false
        }
    }
}

