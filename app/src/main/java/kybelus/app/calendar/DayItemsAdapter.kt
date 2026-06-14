package kybelus.app.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kybelus.app.databinding.ItemDayEventBinding
import kybelus.app.tasks.Task

class DayItemsAdapter(
    private val onDeleteEvent: (Event) -> Unit,
    private val onDeleteTask: (Task) -> Unit
) : RecyclerView.Adapter<DayItemsAdapter.ItemViewHolder>() {

    private var events: List<Event> = emptyList()
    private var tasks: List<Task> = emptyList()

    inner class ItemViewHolder(val binding: ItemDayEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemDayEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (position < events.size) {
            val event = events[position]
            holder.binding.tvItemTitle.text = "🎉 ${event.title}"
            holder.binding.tvItemDesc.text = event.description
            holder.binding.root.setOnLongClickListener {
                android.app.AlertDialog.Builder(holder.binding.root.context, kybelus.app.R.style.CenteredDialog)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete \"${event.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        kybelus.app.notifications.NotificationScheduler.cancelTaskReminder(
                            holder.binding.root.context,
                            event.id + 10000
                        )
                        onDeleteEvent(event)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        } else {
            val task = tasks[position - events.size]
            holder.binding.tvItemTitle.text = "✅ ${task.title}"
            holder.binding.tvItemDesc.text = task.description
            holder.binding.root.setOnLongClickListener {
                android.app.AlertDialog.Builder(holder.binding.root.context, kybelus.app.R.style.CenteredDialog)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete \"${task.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        kybelus.app.notifications.NotificationScheduler.cancelTaskReminder(
                            holder.binding.root.context,
                            task.id
                        )
                        onDeleteTask(task)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }
    }

    override fun getItemCount() = events.size + tasks.size

    fun updateItems(newEvents: List<Event>, newTasks: List<Task>) {
        events = newEvents
        tasks = newTasks
        notifyDataSetChanged()
    }
}