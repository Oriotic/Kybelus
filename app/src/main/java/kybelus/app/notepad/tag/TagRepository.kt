package kybelus.app.notepad.tag

import kybelus.app.KybelusDatabase

class TagRepository(private val db: KybelusDatabase) {

    fun getAllTags(): List<Tag> {
        return db.TagDao().getAllTags()
    }

    fun addTag(Tag: Tag) {
        db.TagDao().insertTag(Tag)
    }

    fun deleteTag(Tag: Tag) {
        db.TagDao().deleteTag(Tag)
    }
}