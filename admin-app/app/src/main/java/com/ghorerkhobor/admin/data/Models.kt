package com.ghorerkhobor.admin.data

data class Message(
    val seq: Int,
    val id: String,
    val deviceId: String,
    val person: String,
    val appPackage: String,
    val appName: String,
    val sender: String,
    val title: String,
    val text: String,
    val conversation: String,
    val postedAt: Long,
    val postedTime: String,
    val imageId: String,
    val savedAt: String
)

data class Device(
    val deviceId: String,
    val person: String,
    val role: String,
    val registeredAt: String,
    val lastSeen: String,
    val lastUpload: String,
    val appVersion: String
)
