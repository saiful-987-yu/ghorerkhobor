package com.ghorerkhobor.user.notification

import android.app.Notification
import android.graphics.Bitmap
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.ghorerkhobor.user.data.AppDatabase
import com.ghorerkhobor.user.data.MessageEntity
import com.ghorerkhobor.user.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class NotificationCaptureService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.get(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (Prefs.isStopped(applicationContext)) return

        val appName = MessagingApps.SUPPORTED[sbn.packageName] ?: return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras ?: Bundle()
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val lastMessage = style?.messages?.lastOrNull()

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val conversation = style?.conversationTitle?.toString() ?: title
        val sender = lastMessage?.person?.name?.toString() ?: title
        val text = lastMessage?.text?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return

        val image = extractImage(extras)

        val entity = MessageEntity(
            notificationKey = sbn.key,
            appPackage = sbn.packageName,
            appName = appName,
            sender = sender,
            title = title,
            text = text,
            conversation = conversation,
            postedAt = sbn.postTime,
            image = image
        )

        scope.launch {
            db.messageDao().insert(entity)
        }
    }

    @Suppress("DEPRECATION")
    private fun extractImage(extras: Bundle): ByteArray? {
        val bitmap = extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            ?: extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON_BIG)
            ?: return null
        return try {
            val maxSize = 480
            val largestSide = maxOf(bitmap.width, bitmap.height)
            val scale = if (largestSide > maxSize) maxSize.toFloat() / largestSide else 1f
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
