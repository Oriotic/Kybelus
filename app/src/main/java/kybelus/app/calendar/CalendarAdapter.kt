package kybelus.app.calendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kybelus.app.databinding.ItemCalendarDayBinding

class CalendarAdapter(
    private var days: List<String?>,
    private val onDayClick: (String) -> Unit,
    private val onDayLongClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var eventDates = emptySet<String>()
    private var taskDates = emptySet<String>()
    private var selectedDate = ""

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return days[position]?.hashCode()?.toLong() ?: position.toLong()
    }

    private fun coloredDot(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    inner class DayViewHolder(val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = days[position]

        if (date == null) {
            holder.binding.tvDay.text = ""
            holder.binding.dotIndicator.visibility = android.view.View.GONE
            return
        }

        val dayNumber = date.split(" ")[1].trimEnd(',')
        holder.binding.tvDay.text = dayNumber

        val hasEvent = eventDates.contains(date)
        val hasTask = taskDates.contains(date)

        val today = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            .format(java.util.Calendar.getInstance().time)

        // Dot color
        when {
            hasEvent && hasTask -> {
                holder.binding.dotIndicator.visibility = android.view.View.VISIBLE
                holder.binding.dotIndicator.background = coloredDot(Color.parseColor("#4CAF50"))
            }
            hasEvent -> {
                holder.binding.dotIndicator.visibility = android.view.View.VISIBLE
                holder.binding.dotIndicator.background = coloredDot(Color.parseColor("#FF9800"))
            }
            hasTask -> {
                holder.binding.dotIndicator.visibility = android.view.View.VISIBLE
                holder.binding.dotIndicator.background = coloredDot(Color.parseColor("#2196F3"))
            }
            !hasEvent && !hasTask && date == today -> {
                holder.binding.dotIndicator.visibility = android.view.View.VISIBLE
                holder.binding.dotIndicator.background = coloredDot(Color.BLACK)
            }
            else -> holder.binding.dotIndicator.visibility = android.view.View.GONE
        }

        // Selected date highlight
        if (date == selectedDate) {
            holder.binding.tvDay.setBackgroundResource(kybelus.app.R.drawable.bg_chip_selected)
            holder.binding.tvDay.setTextColor(Color.WHITE)
        } else {
            holder.binding.tvDay.background = null
            holder.binding.tvDay.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    holder.binding.root.context,
                    kybelus.app.R.color.text_primary)
            )
        }

        holder.binding.root.setOnClickListener { onDayClick(date) }
        holder.binding.root.setOnLongClickListener { onDayLongClick(date); true }
    }

    override fun getItemCount() = days.size

    fun updateDays(newDays: List<String?>, events: Set<String>, tasks: Set<String>, selected: String) {
        days = newDays
        eventDates = events
        taskDates = tasks
        selectedDate = selected
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: String) {
        selectedDate = date
        notifyDataSetChanged()
    }
}