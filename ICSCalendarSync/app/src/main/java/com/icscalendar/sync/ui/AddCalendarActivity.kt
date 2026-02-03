package com.icscalendar.sync.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.gridlayout.widget.GridLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.icscalendar.sync.R
import com.icscalendar.sync.data.CalendarConfig
import com.icscalendar.sync.data.CalendarRepository
import com.icscalendar.sync.databinding.ActivityAddCalendarBinding
import com.icscalendar.sync.service.SyncService
import com.icscalendar.sync.service.SyncWorker

class AddCalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCalendarBinding
    private lateinit var repository: CalendarRepository
    private var calendarId: String? = null
    private var existingCalendar: CalendarConfig? = null
    private var selectedColor: Int = Color.parseColor("#1976D2")

    private val calendarColors = listOf(
        Color.parseColor("#F44336"), // Red
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#673AB7"), // Deep Purple
        Color.parseColor("#3F51B5"), // Indigo
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#00BCD4"), // Cyan
        Color.parseColor("#009688"), // Teal
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#8BC34A"), // Light Green
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#795548")  // Brown
    )

    private var colorViews = mutableListOf<ImageView>()

    companion object {
        const val EXTRA_CALENDAR_ID = "calendar_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CalendarRepository(this)
        calendarId = intent.getStringExtra(EXTRA_CALENDAR_ID)

        setupToolbar()
        setupSyncIntervalSpinner()
        setupColorPicker()
        setupButtons()

        if (calendarId != null) {
            loadExistingCalendar()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (calendarId != null) {
            supportActionBar?.title = getString(R.string.edit)
        }
    }

    private fun setupSyncIntervalSpinner() {
        val intervals = resources.getStringArray(R.array.sync_intervals)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSyncInterval.adapter = adapter
        binding.spinnerSyncInterval.setSelection(2) // Default: 1 hour
    }

    private fun setupColorPicker() {
        val colorPicker = binding.colorPicker
        val size = (44 * resources.displayMetrics.density).toInt()
        val margin = (6 * resources.displayMetrics.density).toInt()

        calendarColors.forEachIndexed { index, color ->
            val colorView = ImageView(this).apply {
                val params = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                    columnSpec = GridLayout.spec(index % 6)
                    rowSpec = GridLayout.spec(index / 6)
                }
                layoutParams = params
                setOnClickListener { selectColor(color) }
            }

            updateColorView(colorView, color, color == selectedColor)
            colorViews.add(colorView)
            colorPicker.addView(colorView)
        }
    }

    private fun updateColorView(view: ImageView, color: Int, selected: Boolean) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (selected) {
                setStroke(
                    (4 * resources.displayMetrics.density).toInt(),
                    ContextCompat.getColor(this@AddCalendarActivity, R.color.black)
                )
            } else {
                setStroke(
                    (2 * resources.displayMetrics.density).toInt(),
                    ContextCompat.getColor(this@AddCalendarActivity, R.color.gray)
                )
            }
        }
        view.setImageDrawable(drawable)
    }

    private fun selectColor(color: Int) {
        selectedColor = color
        colorViews.forEachIndexed { index, view ->
            updateColorView(view, calendarColors[index], calendarColors[index] == selectedColor)
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener { saveCalendar() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun loadExistingCalendar() {
        existingCalendar = repository.getCalendar(calendarId!!)
        existingCalendar?.let { calendar ->
            binding.etCalendarName.setText(calendar.name)
            binding.etIcsUrl.setText(calendar.icsUrl)

            // Set sync interval
            val intervalValues = resources.getStringArray(R.array.sync_interval_values)
            val index = intervalValues.indexOfFirst { it.toInt() == calendar.syncIntervalMinutes }
            if (index >= 0) {
                binding.spinnerSyncInterval.setSelection(index)
            }

            // Set color
            val colorIndex = calendarColors.indexOf(calendar.color)
            if (colorIndex >= 0) {
                selectColor(calendar.color)
            } else {
                selectedColor = calendar.color
            }

            binding.btnDelete.visibility = View.VISIBLE
        }
    }

    private fun saveCalendar() {
        val name = binding.etCalendarName.text.toString().trim()
        val url = binding.etIcsUrl.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            binding.tilCalendarName.error = getString(R.string.validation_name_required)
            return
        }
        binding.tilCalendarName.error = null

        if (url.isEmpty()) {
            binding.tilIcsUrl.error = getString(R.string.validation_url_required)
            return
        }

        if (!Patterns.WEB_URL.matcher(url).matches()) {
            binding.tilIcsUrl.error = getString(R.string.validation_url_invalid)
            return
        }
        binding.tilIcsUrl.error = null

        // Get sync interval
        val intervalValues = resources.getStringArray(R.array.sync_interval_values)
        val intervalMinutes = intervalValues[binding.spinnerSyncInterval.selectedItemPosition].toInt()

        // Create or update calendar config
        val calendar = if (existingCalendar != null) {
            existingCalendar!!.copy(
                name = name,
                icsUrl = url,
                syncIntervalMinutes = intervalMinutes,
                color = selectedColor
            )
        } else {
            CalendarConfig(
                name = name,
                icsUrl = url,
                syncIntervalMinutes = intervalMinutes,
                color = selectedColor
            )
        }

        repository.saveCalendar(calendar)

        // Schedule sync
        SyncWorker.schedulePeriodicSync(this, calendar.id, intervalMinutes)

        // Trigger immediate sync
        SyncWorker.syncNow(this, calendar.id)

        Toast.makeText(this, "Kalender gespeichert", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteCalendar()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCalendar() {
        existingCalendar?.let { calendar ->
            // Cancel scheduled sync
            SyncWorker.cancelSync(this, calendar.id)

            // Delete calendar data
            SyncService(this).deleteCalendarData(calendar)

            // Delete from repository
            repository.deleteCalendar(calendar.id)

            Toast.makeText(this, "Kalender gelöscht", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
