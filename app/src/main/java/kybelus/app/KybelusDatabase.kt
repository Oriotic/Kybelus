package kybelus.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kybelus.app.calendar.Event
import kybelus.app.calendar.EventDao
import kybelus.app.notepad.Note
import kybelus.app.notepad.NoteDao
import kybelus.app.notepad.tag.Tag
import kybelus.app.notepad.tag.TagDao
import kybelus.app.tasks.Task
import kybelus.app.tasks.TaskDao

@Database(
    entities = [Task::class, Note::class, Tag::class, Event::class],
    version = 1,
    exportSchema = false
)
abstract class KybelusDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun TagDao(): TagDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: KybelusDatabase? = null

        fun getDatabase(context: Context): KybelusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KybelusDatabase::class.java,
                    "kybelus_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}