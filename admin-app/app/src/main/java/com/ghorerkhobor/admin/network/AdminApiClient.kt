package com.ghorerkhobor.admin.network

import com.ghorerkhobor.admin.data.Device
import com.ghorerkhobor.admin.data.Message
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AdminApiClient {

    private const val TIMEOUT_MS = 20000

    fun devices(serverUrl: String, adminKey: String): Result<List<Device>> {
        val body = JSONObject()
        body.put("action", "devices")
        body.put("adminKey", adminKey)
        val response = post(serverUrl, body) ?: return Result.failure(Exception("network_error"))
        if (!response.optBoolean("ok", false)) {
            return Result.failure(Exception(response.optString("error", "unknown_error")))
        }
        val array = response.optJSONArray("devices") ?: return Result.success(emptyList())
        val list = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Device(
                deviceId = o.optString("device_id"),
                person = o.optString("person"),
                role = o.optString("role"),
                registeredAt = o.optString("registered_at"),
                lastSeen = o.optString("last_seen"),
                lastUpload = o.optString("last_upload"),
                appVersion = o.optString("app_version")
            )
        }
        return Result.success(list)
    }

    fun list(
        serverUrl: String,
        adminKey: String,
        limit: Int,
        beforePostedAt: Long? = null,
        deviceId: String? = null,
        appPackage: String? = null
    ): Result<ListPage> {
        val body = JSONObject()
        body.put("action", "list")
        body.put("adminKey", adminKey)
        body.put("limit", limit)
        if (beforePostedAt != null) body.put("before", beforePostedAt)
        if (!deviceId.isNullOrBlank()) body.put("device_id", deviceId)
        if (!appPackage.isNullOrBlank()) body.put("app_package", appPackage)
        val response = post(serverUrl, body) ?: return Result.failure(Exception("network_error"))
        if (!response.optBoolean("ok", false)) {
            return Result.failure(Exception(response.optString("error", "unknown_error")))
        }
        val array = response.optJSONArray("items") ?: return Result.success(ListPage(emptyList(), false))
        val items = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Message(
                seq = o.optInt("seq"),
                id = o.optString("id"),
                deviceId = o.optString("device_id"),
                person = o.optString("person"),
                appPackage = o.optString("app_package"),
                appName = o.optString("app_name"),
                sender = o.optString("sender"),
                title = o.optString("title"),
                text = o.optString("text"),
                conversation = o.optString("conversation"),
                postedAt = o.optLong("posted_at"),
                postedTime = o.optString("posted_time"),
                imageId = o.optString("image_id"),
                savedAt = o.optString("saved_at")
            )
        }
        return Result.success(ListPage(items, response.optBoolean("has_more", false)))
    }

    fun image(serverUrl: String, adminKey: String, imageId: String): Result<ByteArray> {
        val body = JSONObject()
        body.put("action", "image")
        body.put("adminKey", adminKey)
        body.put("image_id", imageId)
        val response = post(serverUrl, body) ?: return Result.failure(Exception("network_error"))
        if (!response.optBoolean("ok", false)) {
            return Result.failure(Exception(response.optString("error", "unknown_error")))
        }
        val base64 = response.optString("image", "")
        return try {
            Result.success(android.util.Base64.decode(base64, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            Result.failure(e)
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

data class ListPage(val items: List<Message>, val hasMore: Boolean)
