package kybelus.app.notepad

import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import org.json.JSONArray
import org.json.JSONObject

object SpanSerializer {
    fun toJson(spannable: Spannable): String {
        val text = spannable.toString()
        val spansArray = JSONArray()

        spannable.getSpans(0, spannable.length, StyleSpan::class.java).forEach { span ->
            val start = spannable.getSpanStart(span)
            val end   = spannable.getSpanEnd(span)
            if (start < 0 || end < 0 || start >= end) return@forEach
            val type = when (span.style) {
                android.graphics.Typeface.BOLD   -> "bold"
                android.graphics.Typeface.ITALIC -> "italic"
                else -> return@forEach
            }
            spansArray.put(
                JSONObject().apply {
                    put("type",  type)
                    put("start", start)
                    put("end",   end)
                }
            )
        }

        spannable.getSpans(0, spannable.length, AbsoluteSizeSpan::class.java).forEach { span ->
            val start = spannable.getSpanStart(span)
            val end   = spannable.getSpanEnd(span)
            if (start < 0 || end < 0 || start >= end) return@forEach
            spansArray.put(
                JSONObject().apply {
                    put("type",  "size")
                    put("start", start)
                    put("end",   end)
                    put("size",  span.size.toFloat())
                }
            )
        }

        return JSONObject().apply {
            put("text",  text)
            put("spans", spansArray)
        }.toString()
    }

    fun fromJson(json: String): SpannableString {
        if (!json.trimStart().startsWith("{")) {
            return SpannableString(
                android.text.Html.fromHtml(json,
                    android.text.Html.FROM_HTML_MODE_COMPACT)
            )
        }

        return try {
            val root      = JSONObject(json)
            val text      = root.getString("text")
            val spannable = SpannableString(text)
            val spansArray = root.getJSONArray("spans")

            for (i in 0 until spansArray.length()) {
                val obj   = spansArray.getJSONObject(i)
                val type  = obj.getString("type")
                val start = obj.getInt("start")
                val end   = obj.getInt("end")

                if (start < 0 || end > text.length || start >= end) continue

                when (type) {
                    "bold" -> spannable.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    "italic" -> spannable.setSpan(
                        StyleSpan(android.graphics.Typeface.ITALIC),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    "size" -> {
                        val size = obj.getDouble("size").toFloat()
                        spannable.setSpan(
                            AbsoluteSizeSpan(size.toInt(), true),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }

            spannable
        } catch (e: Exception) {
            SpannableString(json)
        }
    }
    fun isJson(content: String): Boolean = content.trimStart().startsWith("{")
}