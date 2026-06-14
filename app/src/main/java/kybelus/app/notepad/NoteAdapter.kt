package kybelus.app.notepad

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kybelus.app.databinding.ItemNoteBinding

class NoteAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    var isSelectionMode = false
    val selectedNoteIds = mutableSetOf<Int>()

    inner class NoteViewHolder(val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        holder.binding.apply {
            tvNoteTitle.text =
                if (note.isPinned) "📌 ${note.title}" else note.title

            tvNoteContent.text = Html.fromHtml(
                note.content,
                Html.FROM_HTML_MODE_COMPACT
            )

            tvNoteDate.text = note.createdAt
            cardNote.setCardBackgroundColor(note.backgroundColor)

            cbSelect.visibility =
                if (isSelectionMode) View.VISIBLE else View.GONE

            cbSelect.isChecked = selectedNoteIds.contains(note.id)

            cardNote.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(note.id)
                } else {
                    onNoteClick(note)
                }
            }

            cardNote.setOnLongClickListener {
                if (isSelectionMode) {
                    toggleSelection(note.id)
                } else {
                    onNoteLongClick(note)
                }
                true
            }
        }
    }

    private fun toggleSelection(noteId: Int) {
        if (selectedNoteIds.contains(noteId)) {
            selectedNoteIds.remove(noteId)
        } else {
            selectedNoteIds.add(noteId)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedNoteIds.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    fun getSelectedNotes(): List<Note> {
        return notes.filter { selectedNoteIds.contains(it.id) }
    }
}