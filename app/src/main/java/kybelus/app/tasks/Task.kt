package kybelus.app.tasks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val priority: String = "Low",
    val isCompleted: Boolean = false,
    val dueDate: String = "",
    val Tag: String = "General"
)