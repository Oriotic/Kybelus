package kybelus.app.tasks

import kybelus.app.KybelusDatabase

class TaskRepository(private val db: KybelusDatabase) {

    fun getAllTasks(): List<Task> {
        return db.taskDao().getAllTasks()
    }

    fun addTask(task: Task): Long {
        return db.taskDao().insertTask(task)
    }

    fun deleteTask(task: Task) {
        db.taskDao().deleteTask(task)
    }

    fun deleteAllTasks() {
        db.taskDao().deleteAllTasks()
    }

    fun updateTask(task: Task) {
        db.taskDao().updateTask(task)
    }
}
