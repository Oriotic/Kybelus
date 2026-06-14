package kybelus.app.tasks

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import kybelus.app.R
import kybelus.app.SettingsFragment
import kybelus.app.databinding.DialogAddTaskBinding
import kybelus.app.databinding.FragmentTasksBinding

class TasksFragment : Fragment() {

    private lateinit var binding: FragmentTasksBinding
    private val viewModel: TaskViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        setupSort()
        observeTasks()
    }

    private var currentSort = ""

    private fun setupSort() {
        binding.btnSort.setOnClickListener {
            binding.sortOptionsRow.visibility = if (binding.sortOptionsRow.visibility == View.GONE)
                View.VISIBLE else View.GONE
        }

        binding.sortPriority.setOnClickListener {
            if (currentSort == "priority") {
                currentSort = ""
                viewModel.sortTasks("default")
                updateSortChips("")
            } else {
                currentSort = "priority"
                viewModel.sortTasks("priority")
                updateSortChips("priority")
            }
            binding.sortOptionsRow.visibility = View.GONE
        }

        binding.sortDate.setOnClickListener {
            if (currentSort == "date") {
                currentSort = ""
                viewModel.sortTasks("default")
                updateSortChips("")
            } else {
                currentSort = "date"
                viewModel.sortTasks("date")
                updateSortChips("date")
            }
            binding.sortOptionsRow.visibility = View.GONE
        }

        binding.sortCompletion.setOnClickListener {
            if (currentSort == "completion") {
                currentSort = ""
                viewModel.sortTasks("default")
                updateSortChips("")
            } else {
                currentSort = "completion"
                viewModel.sortTasks("completion")
                updateSortChips("completion")
            }
            binding.sortOptionsRow.visibility = View.GONE
        }
    }

    private fun updateSortChips(selected: String) {
        binding.sortPriority.text = if (selected == "priority") "📊 By Priority ✓" else "📊 By Priority"
        binding.sortDate.text = if (selected == "date") "📅 By Due Date ✓" else "📅 By Due Date"
        binding.sortCompletion.text = if (selected == "completion") "✅ Incomplete First ✓" else "✅ Incomplete First"
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            emptyList(),
            onDeleteClick = { task ->
                AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete \"${task.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteTask(task)
                        kybelus.app.notifications.NotificationScheduler.cancelTaskReminder(
                            requireContext(),
                            task.id
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onCompleteClick = { task ->
                val updatedTask = task.copy(isCompleted = !task.isCompleted)
                viewModel.updateTask(updatedTask)
                if (updatedTask.isCompleted) {
                    kybelus.app.notifications.NotificationScheduler.cancelTaskReminder(
                        requireContext(),
                        task.id
                    )
                }
            },
            onEditClick = { task -> showEditTaskDialog(task) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = taskAdapter
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener { showAddTaskDialog() }
    }

    private fun observeTasks() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.updateTasks(tasks)
            val all = viewModel.getAllTasks()
            binding.tvTotalCount.text = all.size.toString()
            binding.tvDoneCount.text = all.count { it.isCompleted }.toString()
            binding.tvPendingCount.text = all.count { !it.isCompleted }.toString()
            if (tasks.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Due Date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec")
            val date = "${months[calendar.get(java.util.Calendar.MONTH)]} " +
                    "${calendar.get(java.util.Calendar.DAY_OF_MONTH)}, ${calendar.get(java.util.Calendar.YEAR)}"
            onDateSelected(date)
        }

        datePicker.show(parentFragmentManager, "date_picker")
    }

    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        val priorities = arrayOf("Low", "Medium", "High")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerPriority.adapter = spinnerAdapter
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        dialogBinding.spinnerPriority.popupBackground.setTint(typedValue.data)
        dialogBinding.etDueDate.setOnClickListener {
            showDatePicker { date -> dialogBinding.etDueDate.setText(date) }
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnAdd.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString()
            val description = dialogBinding.etDescription.text.toString()
            val priority = dialogBinding.spinnerPriority.selectedItem.toString()
            val dueDate = dialogBinding.etDueDate.text.toString()
            val Tag = dialogBinding.etTag.text.toString().ifEmpty { "General" }
            if (title.isNotEmpty()) {
                viewModel.addTask(
                    Task(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate,
                        Tag = Tag
                    )
                ) { id ->
                    val prefs = requireContext().getSharedPreferences(
                        SettingsFragment.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE
                    )
                    if (prefs.getBoolean(SettingsFragment.KEY_REMINDERS_ENABLED, false)
                        && dueDate.isNotEmpty()) {
                        kybelus.app.notifications.NotificationScheduler.scheduleTaskReminder(
                            requireContext(),
                            id.toInt(),
                            title,
                            dueDate
                        )
                    }
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        val priorities = arrayOf("Low", "Medium", "High")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerPriority.adapter = spinnerAdapter
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        dialogBinding.spinnerPriority.popupBackground.setTint(typedValue.data)
        dialogBinding.etDueDate.setOnClickListener {
            showDatePicker { date -> dialogBinding.etDueDate.setText(date) }
        }

        dialogBinding.tvDialogTitle.text = "✏️ Edit Task"
        dialogBinding.etTitle.setText(task.title)
        dialogBinding.etDescription.setText(task.description)
        dialogBinding.etDueDate.setText(task.dueDate)
        dialogBinding.etTag.setText(task.Tag)
        dialogBinding.spinnerPriority.setSelection(priorities.indexOf(task.priority))
        dialogBinding.btnAdd.text = "Save"

        val dialog = AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnAdd.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString()
            val description = dialogBinding.etDescription.text.toString()
            val priority = dialogBinding.spinnerPriority.selectedItem.toString()
            val dueDate = dialogBinding.etDueDate.text.toString()
            val Tag = dialogBinding.etTag.text.toString().ifEmpty { "General" }
            if (title.isNotEmpty()) {
                viewModel.updateTask(task.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    Tag = Tag
                ))
                val prefs = requireContext().getSharedPreferences(
                    SettingsFragment.PREFS_NAME,
                    android.content.Context.MODE_PRIVATE
                )
                if (prefs.getBoolean(SettingsFragment.KEY_REMINDERS_ENABLED, false)
                    && dueDate.isNotEmpty()) {
                    kybelus.app.notifications.NotificationScheduler.scheduleTaskReminder(
                        requireContext(),
                        task.id,
                        title,
                        dueDate
                    )
                } else {
                    kybelus.app.notifications.NotificationScheduler.cancelTaskReminder(
                        requireContext(),
                        task.id
                    )
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}