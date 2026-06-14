package kybelus.app.calendar

import androidx.room.*

@Dao
interface EventDao {

    @Query("SELECT * FROM events")
    fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE date = :date")
    fun getEventsByDate(date: String): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: Event): Long

    @Delete
    fun deleteEvent(event: Event)

    @Update
    fun updateEvent(event: Event)
}