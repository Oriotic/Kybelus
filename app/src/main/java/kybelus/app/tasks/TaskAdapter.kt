package kybelus.app.tasks

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kybelus.app.databinding.ItemTaskBinding

class TaskAdapter(
    private var tasks: List<Task>,
    private val onDeleteClick: (Task) -> Unit,
    private val onCompleteClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.apply {
            tvTitle.text = task.title
            tvDescription.text = task.description
            tvPriority.text = task.priority
            tvDueDate.text = if (task.dueDate.isNotEmpty()) "📅 ${task.dueDate}" else ""
            tvTag.text = if (task.Tag.isNotEmpty()) "# ${task.Tag}" else ""
            cbCompleted.isChecked = task.isCompleted

            if (task.isCompleted) {
                tvTitle.alpha = 0.4f
                tvDescription.alpha = 0.4f
            } else {
                tvTitle.alpha = 1f
                tvDescription.alpha = 1f
            }

            val priorityColor = when (task.priority) {
                "High" -> Color.parseColor("#F44336")
                "Medium" -> Color.parseColor("#FF9800")
                else -> Color.parseColor("#4CAF50")
            }
            (tvPriority.background as GradientDrawable).setColor(priorityColor)

            btnDelete.setOnClickListener { onDeleteClick(task) }
            btnEdit.setOnClickListener { onEditClick(task) }
            cbCompleted.setOnClickListener { onCompleteClick(task) }
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}