package com.icscalendar.sync.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.icscalendar.sync.R
import com.icscalendar.sync.data.CalendarConfig
import com.icscalendar.sync.data.CalendarRepository
import com.icscalendar.sync.databinding.ActivityMainBinding
import com.icscalendar.sync.service.SyncService
import com.icscalendar.sync.service.SyncWorker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: CalendarRepository
    private lateinit var adapter: CalendarAdapter
    private lateinit var syncService: SyncService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            showCalendarList()
        } else {
            showPermissionRequest()
        }
    }

    private val addCalendarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadCalendars()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CalendarRepository(this)
        syncService = SyncService(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupPermissionButton()

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasCalendarPermission()) {
            loadCalendars()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        adapter = CalendarAdapter(
            onSyncClick = { calendar -> syncCalendar(calendar) },
            onDeleteClick = { calendar -> confirmDelete(calendar) },
            onItemClick = { calendar -> editCalendar(calendar) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, AddCalendarActivity::class.java)
            addCalendarLauncher.launch(intent)
        }
    }

    private fun setupPermissionButton() {
        binding.btnGrantPermission.setOnClickListener {
            requestCalendarPermissions()
        }
    }

    private fun checkPermissions() {
        if (hasCalendarPermission()) {
            showCalendarList()
            requestNotificationPermission()
        } else {
            showPermissionRequest()
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCalendarPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    private fun showPermissionRequest() {
        binding.permissionView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        binding.fab.visibility = View.GONE
    }

    private fun showCalendarList() {
        binding.permissionView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
        loadCalendars()
    }

    private fun loadCalendars() {
        val calendars = repository.getCalendars()
        adapter.submitList(calendars)

        if (calendars.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun syncCalendar(calendar: CalendarConfig) {
        Toast.makeText(this, getString(R.string.syncing), Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val result = syncService.syncCalendar(calendar)
            result.fold(
                onSuccess = { count ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.sync_success) + " ($count " + getString(R.string.events_count, count).substringAfter(" ") + ")",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadCalendars()
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.sync_error, error.message ?: "Unknown error"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun confirmDelete(calendar: CalendarConfig) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteCalendar(calendar)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCalendar(calendar: CalendarConfig) {
        // Cancel scheduled sync
        SyncWorker.cancelSync(this, calendar.id)

        // Delete calendar data from device
        syncService.deleteCalendarData(calendar)

        // Delete from repository
        repository.deleteCalendar(calendar.id)

        loadCalendars()
        Toast.makeText(this, "Kalender gelöscht", Toast.LENGTH_SHORT).show()
    }

    private fun editCalendar(calendar: CalendarConfig) {
        val intent = Intent(this, AddCalendarActivity::class.java)
        intent.putExtra(AddCalendarActivity.EXTRA_CALENDAR_ID, calendar.id)
        addCalendarLauncher.launch(intent)
    }
}
