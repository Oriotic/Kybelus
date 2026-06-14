package kybelus.app.notepad.tag

import androidx.room.*

@Dao
interface TagDao {

    @Query("SELECT * FROM Tags")
    fun getAllTags(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTag(Tag: Tag)

    @Delete
    fun deleteTag(Tag: Tag)
}