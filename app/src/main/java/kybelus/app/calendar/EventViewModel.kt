package kybelus.app.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kybelus.app.KybelusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EventRepository(KybelusDatabase.getDatabase(application).eventDao())

    val events = MutableLiveData<List<Event>>()
    val selectedDateEvents = MutableLiveData<List<Event>>()

    init {
        loadAllEvents()
    }

    fun loadAllEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getAllEvents()
            withContext(Dispatchers.Main) {
                events.value = result
            }
        }
    }

    fun loadEventsForDate(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getEventsByDate(date)
            withContext(Dispatchers.Main) {
                selectedDateEvents.value = result
            }
        }
    }

    fun addEvent(event: Event, onEventAdded: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.addEvent(event)
            loadAllEvents()
            withContext(Dispatchers.Main) {
                onEventAdded(id)
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEvent(event)
            loadAllEvents()
        }
    }
}