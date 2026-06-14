package kybelus.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kybelus.app.calendar.EventViewModel
import kybelus.app.databinding.FragmentSettingsBinding
import kybelus.app.notifications.NotificationScheduler
import kybelus.app.tasks.TaskViewModel

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val taskViewModel: TaskViewModel by activityViewModels()
    private val eventViewModel: EventViewModel by activityViewModels()

    companion object {
        const val PREFS_NAME = "kybelus_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_DEFAULT_NOTE_COLOR = "default_note_color"
        const val KEY_DEFAULT_VIEW = "default_view"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupDarkMode(prefs)
        setupReminders(prefs)
        setupDefaultNoteColor(prefs)
        setupDefaultView(prefs)
    }

    private fun setupDarkMode(prefs: SharedPreferences) {
        binding.switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, false)

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupReminders(prefs: SharedPreferences) {
        val isEnabled = prefs.getBoolean(KEY_REMINDERS_ENABLED, false)
        binding.switchReminders.isChecked = isEnabled
        binding.tvReminderTime.visibility = if (isEnabled) View.VISIBLE else View.GONE

        val hour = prefs.getInt(KEY_REMINDER_HOUR, 21)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        updateReminderTimeText(hour, minute)

        binding.switchReminders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, isChecked).apply()
            binding.tvReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            NotificationScheduler.rescheduleAll(
                requireContext(),
                taskViewModel.tasks.value ?: emptyList(),
                eventViewModel.events.value ?: emptyList()
            )
        }

        binding.tvReminderTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setHour(prefs.getInt(KEY_REMINDER_HOUR, 21))
                .setMinute(prefs.getInt(KEY_REMINDER_MINUTE, 0))
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setTitleText("Reminder Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                val h = picker.hour
                val m = picker.minute
                prefs.edit()
                    .putInt(KEY_REMINDER_HOUR, h)
                    .putInt(KEY_REMINDER_MINUTE, m)
                    .apply()
                updateReminderTimeText(h, m)
                NotificationScheduler.rescheduleAll(
                    requireContext(),
                    taskViewModel.tasks.value ?: emptyList(),
                    eventViewModel.events.value ?: emptyList()
                )
            }

            picker.show(parentFragmentManager, "time_picker")
        }
    }

    private fun updateReminderTimeText(hour: Int, minute: Int) {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val displayMinute = String.format("%02d", minute)
        binding.tvReminderTime.text = "$displayHour:$displayMinute $amPm"
    }

    private fun setupDefaultNoteColor(prefs: SharedPreferences) {
        binding.colorPickerRow.removeAllViews()
        val colors = listOf(
            Color.WHITE,
            Color.parseColor("#FFE4E4"),
            Color.parseColor("#E4F0FF"),
            Color.parseColor("#E4FFE9"),
            Color.parseColor("#FFF8E4"),
            Color.parseColor("#F3E4FF")
        )

        val savedColor = prefs.getInt(KEY_DEFAULT_NOTE_COLOR, Color.WHITE)

        colors.forEach { color ->
            val circle = View(requireContext()).apply {
                val size = resources.getDimensionPixelSize(R.dimen.color_circle_size)
                layoutParams = ViewGroup.LayoutParams(size, size).also {
                    (it as? LinearLayout.LayoutParams)?.setMargins(8, 0, 8, 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(
                        4,
                        if (color == savedColor) Color.parseColor("#6650A4")
                        else Color.parseColor("#CCCCCC")
                    )
                }
                setOnClickListener {
                    prefs.edit().putInt(KEY_DEFAULT_NOTE_COLOR, color).apply()
                    setupDefaultNoteColor(prefs)
                }
            }
            binding.colorPickerRow.addView(circle)
        }
    }

    private fun setupDefaultView(prefs: SharedPreferences) {
        val views = listOf("Tasks", "Notepad", "Calendar")
        val saved = prefs.getString(KEY_DEFAULT_VIEW, "Tasks")
        binding.tvDefaultView.text = saved

        binding.tvDefaultView.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
                .setTitle("Default View")
                .setItems(views.toTypedArray()) { _, which ->
                    prefs.edit().putString(KEY_DEFAULT_VIEW, views[which]).apply()
                    binding.tvDefaultView.text = views[which]
                }
                .show()
        }
    }
}