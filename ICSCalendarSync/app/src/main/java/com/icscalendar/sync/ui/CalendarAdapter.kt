package com.icscalendar.sync.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.icscalendar.sync.R
import com.icscalendar.sync.data.CalendarConfig
import com.icscalendar.sync.databinding.ItemCalendarBinding
import java.text.SimpleDateFormat
import java.util.*

class CalendarAdapter(
    private val onSyncClick: (CalendarConfig) -> Unit,
    private val onDeleteClick: (CalendarConfig) -> Unit,
    private val onItemClick: (CalendarConfig) -> Unit
) : ListAdapter<CalendarConfig, CalendarAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCalendarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(calendar: CalendarConfig) {
            binding.tvCalendarName.text = calendar.name
            binding.tvUrl.text = calendar.icsUrl

            // Set color indicator
            val colorDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 4f * binding.root.context.resources.displayMetrics.density
                setColor(calendar.color)
            }
            binding.colorIndicator.background = colorDrawable

            // Last sync time
            val context = binding.root.context
            if (calendar.lastSync > 0) {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val lastSyncText = dateFormat.format(Date(calendar.lastSync))
                binding.tvLastSync.text = context.getString(R.string.last_sync, lastSyncText)
            } else {
                binding.tvLastSync.text = context.getString(R.string.never_synced)
            }

            // Events count
            binding.tvEventsCount.text = context.getString(R.string.events_count, calendar.eventsCount)

            // Click listeners
            binding.root.setOnClickListener { onItemClick(calendar) }
            binding.btnSync.setOnClickListener { onSyncClick(calendar) }
            binding.btnDelete.setOnClickListener { onDeleteClick(calendar) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CalendarConfig>() {
        override fun areItemsTheSame(oldItem: CalendarConfig, newItem: CalendarConfig): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarConfig, newItem: CalendarConfig): Boolean {
            return oldItem == newItem
        }
    }
}
