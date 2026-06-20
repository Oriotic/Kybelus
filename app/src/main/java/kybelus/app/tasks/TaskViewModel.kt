package kybelus.app.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kybelus.app.KybelusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(KybelusDatabase.getDatabase(application))

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getAllTasks()
            withContext(Dispatchers.Main) {
                _tasks.value = result
            }
        }
    }

    fun getAllTasks(): List<Task> {
        return _tasks.value ?: emptyList()
    }

    fun addTask(task: Task, onTaskAdded: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.addTask(task)
            loadTasks()
            withContext(Dispatchers.Main) {
                onTaskAdded(id)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
            loadTasks()
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllTasks()
            loadTasks()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task)
            loadTasks()
        }
    }

    fun filterTasks(filter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllTasks()
            val filtered = when (filter) {
                "completed" -> all.filter { it.isCompleted }
                "high" -> all.filter { it.priority == "High" }
                else -> all
            }
            withContext(Dispatchers.Main) {
                _tasks.value = filtered
            }
        }
    }

    fun sortTasks(sortBy: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllTasks()
            val sorted = when (sortBy) {
                "priority" -> all.sortedByDescending {
                    when (it.priority) {
                        "High" -> 3
                        "Medium" -> 2
                        else -> 1
                    }
                }
                "date" -> all.sortedBy { it.dueDate }
                "completion" -> all.sortedBy { it.isCompleted }
                else -> all
            }
            withContext(Dispatchers.Main) {
                _tasks.value = sorted
            }
        }
    }
}
