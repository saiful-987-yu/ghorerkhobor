package com.ghorerkhobor.user.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "messages", indices = [Index(value = ["notificationKey"], unique = true)])
class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationKey: String,
    val appPackage: String,
    val appName: String,
    val sender: String,
    val title: String,
    val text: String,
    val conversation: String,
    val postedAt: Long,
    val image: ByteArray?,
    val savedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
