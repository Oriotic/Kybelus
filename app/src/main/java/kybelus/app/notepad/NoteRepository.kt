package kybelus.app.notepad

import kybelus.app.KybelusDatabase

class NoteRepository(private val db: KybelusDatabase) {

    fun getAllNotes(): List<Note> {
        return db.noteDao().getAllNotes()
    }

    fun addNote(note: Note) {
        db.noteDao().insertNote(note)
    }

    fun deleteNote(note: Note) {
        db.noteDao().deleteNote(note)
    }

    fun deleteNotes(notes: List<Note>) {
        db.noteDao().deleteNotes(notes)
    }

    fun updateNote(note: Note) {
        db.noteDao().updateNote(note)
    }

    fun updateNotes(notes: List<Note>) {
        db.noteDao().updateNotes(notes)
    }
}