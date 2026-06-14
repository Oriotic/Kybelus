package kybelus.app.notepad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kybelus.app.KybelusDatabase
import kybelus.app.notepad.tag.Tag
import kybelus.app.notepad.tag.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository(KybelusDatabase.getDatabase(application))
    private val tagRepository = TagRepository(KybelusDatabase.getDatabase(application))

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    private val _tags = MutableLiveData<List<Tag>>()
    val tags: LiveData<List<Tag>> = _tags

    private var currentTagFilter = -1

    init {
        loadNotes()
        loadTags()
    }

    private fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllNotes()
            val filtered = if (currentTagFilter == -1) all
            else all.filter { it.tagId == currentTagFilter }
            val sorted = filtered.sortedByDescending { it.isPinned }
            withContext(Dispatchers.Main) {
                _notes.value = sorted
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = tagRepository.getAllTags()
            withContext(Dispatchers.Main) {
                _tags.value = result
            }
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllNotes()
            val filtered = if (currentTagFilter == -1) all
            else all.filter { it.tagId == currentTagFilter }
            val result = if (query.isEmpty()) {
                filtered.sortedByDescending { it.isPinned }
            } else {
                filtered.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            android.text.Html.fromHtml(it.content,
                                android.text.Html.FROM_HTML_MODE_COMPACT)
                                .toString().contains(query, ignoreCase = true)
                }
            }
            withContext(Dispatchers.Main) {
                _notes.value = result
            }
        }
    }

    fun filterByTag(tagId: Int) {
        currentTagFilter = tagId
        loadNotes()
    }

    fun addNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addNote(note)
            loadNotes()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
            loadNotes()
        }
    }

    fun deleteNotes(notes: List<Note>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotes(notes)
            loadNotes()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note)
            loadNotes()
        }
    }

    fun updateNotes(notes: List<Note>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNotes(notes)
            loadNotes()
        }
    }

    fun pinNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
            loadNotes()
        }
    }

    fun addTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.addTag(tag)
            loadTags()
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.deleteTag(tag)
            loadNotes()
            loadTags()
        }
    }

    fun getAllTags(): List<Tag> {
        return _tags.value ?: emptyList()
    }
}
