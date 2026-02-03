package com.icscalendar.sync.service

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.icscalendar.sync.data.CalendarRepository
import kotlinx.coroutines.runBlocking

class CalendarSyncAdapter(
    context: Context,
    autoInitialize: Boolean
) : AbstractThreadedSyncAdapter(context, autoInitialize) {

    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?
    ) {
        // Sync all calendars
        runBlocking {
            val repository = CalendarRepository(context)
            val syncService = SyncService(context)

            for (calendar in repository.getCalendars()) {
                try {
                    syncService.syncCalendar(calendar)
                } catch (e: Exception) {
                    syncResult?.stats?.numIoExceptions = (syncResult?.stats?.numIoExceptions ?: 0) + 1
                }
            }
        }
    }
}
