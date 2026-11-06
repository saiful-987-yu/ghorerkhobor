package com.ghorerkhobor.user

import android.app.Application
import com.ghorerkhobor.user.data.Prefs
import com.ghorerkhobor.user.sync.SyncScheduler

class GhorerKhoborApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Prefs.registerCrashAndCheckShouldStop(applicationContext)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        if (!Prefs.isStopped(this)) {
            SyncScheduler.schedulePeriodic(this)
        }
    }
}
