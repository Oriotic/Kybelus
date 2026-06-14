package kybelus.app.calendar

class EventRepository(private val dao: EventDao) {

    fun getAllEvents(): List<Event> = dao.getAllEvents()

    fun getEventsByDate(date: String): List<Event> = dao.getEventsByDate(date)

    fun addEvent(event: Event): Long = dao.insertEvent(event)

    fun deleteEvent(event: Event) = dao.deleteEvent(event)

    fun updateEvent(event: Event) = dao.updateEvent(event)
}