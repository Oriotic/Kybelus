package kybelus.app.calendar

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kybelus.app.R
import kybelus.app.SettingsFragment
import kybelus.app.databinding.FragmentCalendarBinding
import kybelus.app.tasks.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var binding: FragmentCalendarBinding
    private val eventViewModel: EventViewModel by activityViewModels()
    private val taskViewModel: TaskViewModel by activityViewModels()

    private val calendar = Calendar.getInstance()
    private var selectedDate: String = ""
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var dayItemsAdapter: DayItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? kybelus.app.MainActivity)?.binding?.drawerLayout
            ?.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        (activity as? kybelus.app.MainActivity)?.binding?.toolbar
            ?.setNavigationOnClickListener {
                (activity as? kybelus.app.MainActivity)?.binding?.drawerLayout
                    ?.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
                (activity as? kybelus.app.MainActivity)?.binding?.drawerLayout
                    ?.openDrawer(androidx.core.view.GravityCompat.START)
    }
}

    override fun onPause() {
        super.onPause()
        (activity as? kybelus.app.MainActivity)?.binding?.drawerLayout
            ?.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
        (activity as? kybelus.app.MainActivity)?.setupDrawer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedDate = formatDate(Calendar.getInstance())

        setupCalendarGrid()

        binding.rvCalendar.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            val gestureDetector = android.view.GestureDetector(requireContext(),
                object : android.view.GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(e1: android.view.MotionEvent?,
                                         e2: android.view.MotionEvent, velocityX: Float,
                                         velocityY: Float): Boolean {
                        val diff = (e1?.x ?: 0f) - e2.x
                        if (Math.abs(diff) > 100) {
                            if (diff > 0) {
                                calendar.add(Calendar.MONTH, 1)
                                refreshCalendar()
                                binding.rvCalendar.startAnimation(
                                    android.view.animation.AnimationUtils.loadAnimation(
                                        requireContext(), R.anim.slide_left)
                                )
                            } else {
                                calendar.add(Calendar.MONTH, -1)
                                refreshCalendar()
                                binding.rvCalendar.startAnimation(
                                    android.view.animation.AnimationUtils.loadAnimation(
                                        requireContext(), R.anim.slide_right)
                                )
                            }
                            return true
                        }
                        return false
                    }
                })

            override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
        })

        setupDayItemsList()
        observeData()
        loadSelectedDate()
    }

    private fun setupCalendarGrid() {
        calendarAdapter = CalendarAdapter(
            days = emptyList(),
            onDayClick = { date -> onDateSelected(date) },
            onDayLongClick = { date -> showAddEventDialog(date) }
        )
        binding.rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendar.adapter = calendarAdapter
        refreshCalendar()
    }

    private fun setupDayItemsList() {
        dayItemsAdapter = DayItemsAdapter(
            onDeleteEvent = { event -> eventViewModel.deleteEvent(event) },
            onDeleteTask = { task -> taskViewModel.deleteTask(task) }
        )
        binding.rvDayItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDayItems.adapter = dayItemsAdapter
    }

    private fun observeData() {
        eventViewModel.events.observe(viewLifecycleOwner) {
            refreshCalendar()
            loadSelectedDate()
        }
        taskViewModel.tasks.observe(viewLifecycleOwner) {
            refreshCalendar()
            loadSelectedDate()
        }
        eventViewModel.selectedDateEvents.observe(viewLifecycleOwner) { events ->
            val tasks = taskViewModel.tasks.value
                ?.filter { it.dueDate == selectedDate } ?: emptyList()
            dayItemsAdapter.updateItems(events, tasks)
        }
    }

    private fun onDateSelected(date: String) {
        selectedDate = date
        calendarAdapter.setSelectedDate(date)
        loadSelectedDate()
    }

    private fun loadSelectedDate() {
        binding.tvSelectedDate.text = selectedDate
        eventViewModel.loadEventsForDate(selectedDate)
    }

    private fun refreshCalendar() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = monthFormat.format(calendar.time)

        val days = buildDaysList()
        val eventDates = eventViewModel.events.value?.map { it.date }?.toSet() ?: emptySet()
        val taskDates = taskViewModel.tasks.value?.map { it.dueDate }?.toSet() ?: emptySet()

        calendarAdapter.updateDays(days, eventDates, taskDates, selectedDate)
    }

    private fun buildDaysList(): List<String?> {
        val days = mutableListOf<String?>()
        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
        repeat(firstDayOfWeek) { days.add(null) }
        for (day in 1..daysInMonth) {
            temp.set(Calendar.DAY_OF_MONTH, day)
            days.add(formatDate(temp))
        }
        return days
    }

    private fun formatDate(cal: Calendar): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(cal.time)
    }

    fun showAddEventDialog(date: String) {
        val dialogBinding = kybelus.app.databinding.DialogAddEventBinding.inflate(layoutInflater)

        AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
            .setTitle("New Event — $date")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val title = dialogBinding.etEventTitle.text.toString()
                if (title.isNotEmpty()) {
                    val event = Event(title = title, description = dialogBinding.etEventDescription.text.toString(), date = date)
                    eventViewModel.addEvent(event) { id ->
                        val prefs = requireContext().getSharedPreferences(
                            SettingsFragment.PREFS_NAME,
                            android.content.Context.MODE_PRIVATE
                        )
                        if (prefs.getBoolean(SettingsFragment.KEY_REMINDERS_ENABLED, false)) {
                            kybelus.app.notifications.NotificationScheduler.scheduleTaskReminder(
                                requireContext(),
                                id.toInt() + 10000,
                                title,
                                date
                            )
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun onFabClicked() {
        showAddEventDialog(selectedDate)
    }
}