package com.ghorerkhobor.user.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghorerkhobor.user.data.AppDatabase
import com.ghorerkhobor.user.data.Prefs

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (Prefs.isStopped(context)) return Result.success()

        val serverUrl = Prefs.serverUrl(context)
        val apiKey = Prefs.apiKey(context)
        if (serverUrl.isBlank() || apiKey.isBlank()) return Result.success()

        val dao = AppDatabase.get(context).messageDao()
        val deviceId = Prefs.deviceId(context)

        if (!Prefs.isRegistered(context)) {
            val person = Prefs.personName(context)
            val role = Prefs.role(context)
            val registerResult = ApiClient.register(serverUrl, apiKey, deviceId, person, role)
            if (registerResult.isFailure) {
                Prefs.setLastSync(context, System.currentTimeMillis(), "ব্যর্থ: রেজিস্ট্রেশন হয়নি")
                return Result.retry()
            }
            Prefs.setRegistered(context, true)
        }

        var totalSynced = 0
        while (true) {
            val batch = dao.unsyncedBatch(150)
            if (batch.isEmpty()) break
            val uploadResult = ApiClient.upload(serverUrl, apiKey, deviceId, batch)
            if (uploadResult.isFailure) {
                val message = if (totalSynced > 0) {
                    "আংশিক সফল: $totalSynced টি পাঠানো হয়েছে"
                } else {
                    "ব্যর্থ: সংযোগ করা যায়নি"
                }
                Prefs.setLastSync(context, System.currentTimeMillis(), message)
                return Result.retry()
            }
            dao.markSynced(batch.map { it.id })
            totalSynced += batch.size
            if (batch.size < 150) break
        }

        val resultText = if (totalSynced > 0) "সফল: $totalSynced টি মেসেজ পাঠানো হয়েছে" else "সফল: নতুন কিছু ছিল না"
        Prefs.setLastSync(context, System.currentTimeMillis(), resultText)
        return Result.success()
    }
}
