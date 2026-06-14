package kybelus.app.notepad

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: String = "",
    val backgroundColor: Int = android.graphics.Color.WHITE,
    val isPinned: Boolean = false,
    val tagId: Int = -1
)