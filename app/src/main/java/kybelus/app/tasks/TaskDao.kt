package kybelus.app.tasks

import androidx.room.*

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: Task): Long

    @Delete
    fun deleteTask(task: Task)

    @Update
    fun updateTask(task: Task)
}