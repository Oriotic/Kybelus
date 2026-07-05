package kybelus.app.notepad

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

class SelectionAwareEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextInputEditText(context, attrs) {

    var onSelectionChangedListener: ((selStart: Int, selEnd: Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }
}