package com.icscalendar.sync.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class CalendarSyncAdapterService : Service() {

    private lateinit var syncAdapter: CalendarSyncAdapter

    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = CalendarSyncAdapter(applicationContext, true)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return syncAdapter.syncAdapterBinder
    }

    companion object {
        private val syncAdapterLock = Any()
    }
}
