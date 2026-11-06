package com.ghorerkhobor.user.sync

import android.util.Base64
import com.ghorerkhobor.user.data.MessageEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val TIMEOUT_MS = 20000

    fun register(serverUrl: String, apiKey: String, deviceId: String, person: String, role: String): Result<Unit> {
        val body = JSONObject()
        body.put("action", "register")
        body.put("key", apiKey)
        body.put("device_id", deviceId)
        body.put("person", person)
        body.put("role", role)
        body.put("app_version", "1.0.0")
        val response = post(serverUrl, body) ?: return Result.failure(Exception("network_error"))
        return if (response.optBoolean("ok", false)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.optString("error", "unknown_error")))
        }
    }

    fun upload(serverUrl: String, apiKey: String, deviceId: String, items: List<MessageEntity>): Result<Int> {
        val body = JSONObject()
        body.put("action", "upload")
        body.put("key", apiKey)
        body.put("device_id", deviceId)
        val array = JSONArray()
        items.forEach { msg ->
            val item = JSONObject()
            item.put("id", msg.notificationKey)
            item.put("app_package", msg.appPackage)
            item.put("app_name", msg.appName)
            item.put("sender", msg.sender)
            item.put("title", msg.title)
            item.put("text", msg.text)
            item.put("conversation", msg.conversation)
            item.put("posted_at", msg.postedAt)
            if (msg.image != null) {
                item.put("image", Base64.encodeToString(msg.image, Base64.NO_WRAP))
            }
            array.put(item)
        }
        body.put("items", array)
        val response = post(serverUrl, body) ?: return Result.failure(Exception("network_error"))
        return if (response.optBoolean("ok", false)) {
            Result.success(response.optInt("saved", 0))
        } else {
            Result.failure(Exception(response.optString("error", "unknown_error")))
        }
    }

    private fun post(serverUrl: String, body: JSONObject): JSONObject? {
        if (serverUrl.isBlank()) return null
        return try {
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.instanceFollowRedirects = true
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: return null
            JSONObject(text)
        } catch (e: Exception) {
            null
        }
    }
}
