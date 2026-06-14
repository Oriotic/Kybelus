package kybelus.app

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import kybelus.app.databinding.ActivityMainBinding
import kybelus.app.tasks.TasksFragment
import kybelus.app.tasks.TaskViewModel
import kybelus.app.notepad.NotepadFragment
import kybelus.app.calendar.CalendarFragment
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.os.Build

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(SettingsFragment.KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDrawer()

        if (savedInstanceState == null) {
            val defaultView = prefs.getString(SettingsFragment.KEY_DEFAULT_VIEW, "Tasks")
            when (defaultView) {
                "Tasks" -> loadFragment(TasksFragment(), "Tasks")
                "Notepad" -> loadFragment(NotepadFragment(), "Notepad")
                "Calendar" -> loadFragment(CalendarFragment(), "Calendar")
                else -> loadFragment(TasksFragment(), "Tasks")
            }
        }

        val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    internal fun setupDrawer() {
        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set default checked item
        binding.navigationView.setCheckedItem(R.id.nav_tasks)

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> loadFragment(TasksFragment(), "Tasks")
                R.id.nav_notepad -> loadFragment(NotepadFragment(), "Notepad")
                R.id.nav_calendar -> loadFragment(CalendarFragment(), "Calendar")
                R.id.nav_settings -> loadFragment(SettingsFragment(), "Settings")
            }
            binding.navigationView.setCheckedItem(item.itemId)
            binding.drawerLayout.closeDrawers()
            true
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment, title: String) {
        supportActionBar?.title = "🌙 $title"
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}