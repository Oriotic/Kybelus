package kybelus.app.notepad

import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kybelus.app.databinding.FragmentNoteEditorBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kybelus.app.R
import kybelus.app.SettingsFragment

class NoteEditorFragment(
    private val note: Note? = null,
    private val onSave: () -> Unit
) : Fragment() {
    private var typingBold = false
    private var typingItalic = false
    private var typingFontSize = 16f

    private var listPrefix = ""
    private var isWatcherEnabled = true
    private lateinit var binding: FragmentNoteEditorBinding
    private val viewModel: NoteViewModel by activityViewModels()
    private var selectedColor = android.graphics.Color.WHITE

    private data class EditorSnapshot(val json: String, val cursorStart: Int, val cursorEnd: Int)

    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private val MAX_HISTORY = 50
    private var isRestoringSnapshot = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? kybelus.app.MainActivity)?.binding?.appBarLayout?.visibility = View.GONE
        (activity as? kybelus.app.MainActivity)?.binding?.drawerLayout
            ?.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onPause() {
        super.onPause()
        (activity as? kybelus.app.MainActivity)?.binding?.appBarLayout?.visibility = View.VISIBLE
        (activity as? kybelus.app.MainActivity)?.binding?.drawerLayout
            ?.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        note?.let {
            binding.etEditorTitle.setText(it.title)
            isWatcherEnabled = false
            binding.etEditorContent.setText(
                SpanSerializer.fromJson(it.content),
                android.widget.TextView.BufferType.SPANNABLE
            )
            isWatcherEnabled = true
        } ?: run {
            val prefs = requireContext().getSharedPreferences(
                SettingsFragment.PREFS_NAME,
                android.content.Context.MODE_PRIVATE
            )
            selectedColor = prefs.getInt(
                SettingsFragment.KEY_DEFAULT_NOTE_COLOR,
                android.graphics.Color.WHITE
            )
            binding.etEditorContent.setText("", android.widget.TextView.BufferType.SPANNABLE)
        }

        setupTextWatcher()
        setupToolbar()
        setupColorPicker(note)
        setupSelectionTracking()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSaveNote.setOnClickListener {
            val title = binding.etEditorTitle.text.toString()
            val content = SpanSerializer.toJson(
                binding.etEditorContent.text as Spannable
            )
            val finalTitle = if (title.isNotEmpty()) title else ""

            if (title.isNotEmpty() || content.isNotEmpty()) {
                if (note == null) {
                    viewModel.addNote(
                        Note(
                            title = finalTitle,
                            content = content,
                            createdAt = SimpleDateFormat(
                                "MMM dd, yyyy",
                                Locale.getDefault()
                            ).format(Date()),
                            backgroundColor = selectedColor
                        )
                    )
                } else {
                    viewModel.updateNote(
                        note.copy(
                            title = finalTitle,
                            content = content,
                            backgroundColor = selectedColor
                        )
                    )
                }
                onSave()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupTextWatcher() {
        binding.etEditorContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isWatcherEnabled || s == null) return
                val prevCh = if (start > 0) s.getOrNull(start - 1) else null
                if (prevCh == null || prevCh == ' ' || prevCh == '\n' || prevCh == '.' || prevCh == ',' || prevCh == '!' || prevCh == '?') {
                    pushSnapshot()
                } else if (undoStack.isEmpty() && s.isNotEmpty()) {
                    // First edit inside existing text — capture the original state
                    pushSnapshot()
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isWatcherEnabled || s == null) return
                val end = binding.etEditorContent.selectionEnd
                val start = end - 1
                if (start < 0) return

                if (listPrefix.isNotEmpty() && s.isNotEmpty() && end > 0) {
                    if (s[start] == '\n') {
                        isWatcherEnabled = false
                        s.insert(end, listPrefix)
                        isWatcherEnabled = true
                    }
                }

                enforceStyleOnInsertedRange(s, start, end, android.graphics.Typeface.BOLD, typingBold)
                enforceStyleOnInsertedRange(s, start, end, android.graphics.Typeface.ITALIC, typingItalic)
                enforceSizeOnInsertedRange(s, start, end, typingFontSize)
            }
        })
    }

    private fun enforceStyleOnInsertedRange(s: Editable, start: Int, end: Int, style: Int, shouldBeStyled: Boolean) {
        if (shouldBeStyled) {
            s.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return
        }
        s.getSpans(start, end, StyleSpan::class.java)
            .filter { it.style == style }
            .forEach { span ->
                val sStart = s.getSpanStart(span)
                val sEnd = s.getSpanEnd(span)
                s.removeSpan(span)
                if (sStart < start) s.setSpan(StyleSpan(style), sStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (sEnd > end) s.setSpan(StyleSpan(style), end, sEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
    }

    private fun enforceSizeOnInsertedRange(s: Editable, start: Int, end: Int, targetSize: Float) {
        if (targetSize != 16f) {
            s.setSpan(AbsoluteSizeSpan(targetSize.toInt(), true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return
        }
        s.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach { span ->
            val sStart = s.getSpanStart(span)
            val sEnd = s.getSpanEnd(span)
            s.removeSpan(span)
            if (sStart < start) s.setSpan(AbsoluteSizeSpan(span.size, true), sStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (sEnd > end) s.setSpan(AbsoluteSizeSpan(span.size, true), end, sEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun setupSelectionTracking() {
        binding.etEditorContent.onSelectionChangedListener = { start, end ->
            if (isWatcherEnabled) updateToolbarStateForSelection(start, end)
        }
        updateToolbarStateForSelection(
            binding.etEditorContent.selectionStart,
            binding.etEditorContent.selectionEnd
        )
    }

    private fun updateToolbarStateForSelection(selStart: Int, selEnd: Int) {
        val editable = binding.etEditorContent.text ?: return
        if (selStart == selEnd) {
            val (bold, italic, size) = stylesAtCursor(editable, selStart)
            typingBold = bold
            typingItalic = italic
            typingFontSize = size
        } else {
            val start = minOf(selStart, selEnd)
            val end = maxOf(selStart, selEnd)
            typingBold = rangeIsFullyStyled(editable, start, end, android.graphics.Typeface.BOLD)
            typingItalic = rangeIsFullyStyled(editable, start, end, android.graphics.Typeface.ITALIC)
            typingFontSize = uniformSizeInRange(editable, start, end) ?: 16f
        }
        refreshToolbarUI()
    }

    private fun stylesAtCursor(editable: Editable, cursor: Int): Triple<Boolean, Boolean, Float> {
        if (editable.isEmpty()) return Triple(false, false, 16f)
        val checkPos = (cursor - 1).coerceIn(0, editable.length - 1)
        val end = checkPos + 1
        val styleSpans = editable.getSpans(checkPos, end, StyleSpan::class.java)
        val isBold = styleSpans.any { it.style == android.graphics.Typeface.BOLD }
        val isItalic = styleSpans.any { it.style == android.graphics.Typeface.ITALIC }
        val size = editable.getSpans(checkPos, end, AbsoluteSizeSpan::class.java).firstOrNull()?.size?.toFloat() ?: 16f
        return Triple(isBold, isItalic, size)
    }

    private fun uniformSizeInRange(editable: Editable, start: Int, end: Int): Float? {
        if (start >= end) return null
        val spans = editable.getSpans(start, end, AbsoluteSizeSpan::class.java)
        if (spans.isEmpty()) return null
        val distinctSizes = spans.map { it.size.toFloat() }.distinct()
        if (distinctSizes.size != 1) return null
        val covered = spans.map { editable.getSpanStart(it)..editable.getSpanEnd(it) }
        for (i in start until end) if (covered.none { i in it }) return null
        return distinctSizes.first()
    }

    private fun refreshToolbarUI() {
        binding.btnBold.alpha = if (typingBold) 1f else 0.4f
        binding.btnItalic.alpha = if (typingItalic) 1f else 0.4f
        binding.btnFontSize.alpha = if (typingFontSize != 16f) 1f else 0.4f
        binding.btnFontSize.text = when (typingFontSize) {
            14f  -> "A-"
            18f  -> "A+"
            24f  -> "A²"
            else -> "A"
        }
    }

    private fun rangeIsFullyStyled(editable: Editable, start: Int, end: Int, style: Int): Boolean {
        if (start >= end) return false
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
            .filter { it.style == style }
        if (spans.isEmpty()) return false
        val covered = spans.map { editable.getSpanStart(it)..editable.getSpanEnd(it) }
        for (i in start until end) {
            if (covered.none { i in it }) return false
        }
        return true
    }

    private fun applyOrRemoveStyle(style: Int) {
        val editable = binding.etEditorContent.text ?: return
        val selStart = binding.etEditorContent.selectionStart
        val selEnd   = binding.etEditorContent.selectionEnd

        if (selStart == selEnd) {
            if (style == android.graphics.Typeface.BOLD) typingBold = !typingBold
            else typingItalic = !typingItalic
            refreshToolbarUI()
            return
        }

        pushSnapshot()
        val alreadyStyled = rangeIsFullyStyled(editable, selStart, selEnd, style)
        if (alreadyStyled) {
            editable.getSpans(selStart, selEnd, StyleSpan::class.java)
                .filter { it.style == style }
                .forEach { span ->
                    val sStart = editable.getSpanStart(span)
                    val sEnd   = editable.getSpanEnd(span)
                    editable.removeSpan(span)
                    if (sStart < selStart) {
                        editable.setSpan(
                            StyleSpan(style), sStart, selStart,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    if (sEnd > selEnd) {
                        editable.setSpan(
                            StyleSpan(style), selEnd, sEnd,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
        } else {
            editable.setSpan(
                StyleSpan(style), selStart, selEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Update button visual to match state
        if (style == android.graphics.Typeface.BOLD) typingBold = !alreadyStyled
        else typingItalic = !alreadyStyled
        refreshToolbarUI()
    }

    private fun applyOrRemoveFontSize(size: Float) {
        val editable  = binding.etEditorContent.text ?: return
        val selStart  = binding.etEditorContent.selectionStart
        val selEnd    = binding.etEditorContent.selectionEnd

        if (selStart == selEnd) {
            typingFontSize = when (typingFontSize) {
                16f -> 18f; 18f -> 24f; 24f -> 14f; else -> 16f
            }
            refreshToolbarUI()
            return
        }

        editable.getSpans(selStart, selEnd, AbsoluteSizeSpan::class.java).forEach { span ->
            val sStart = editable.getSpanStart(span)
            val sEnd   = editable.getSpanEnd(span)
            editable.removeSpan(span)
            if (sStart < selStart) {
                editable.setSpan(
                    AbsoluteSizeSpan(span.size, true), sStart, selStart,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (sEnd > selEnd) {
                editable.setSpan(
                    AbsoluteSizeSpan(span.size, true), selEnd, sEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        if (size != 16f) {
            editable.setSpan(
                AbsoluteSizeSpan(size.toInt(), true), selStart, selEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        typingFontSize = size
        refreshToolbarUI()
    }

    private fun setupColorPicker(note: Note?) {
        selectedColor = note?.backgroundColor ?: selectedColor

        val colors = mapOf(
            binding.color1 to android.graphics.Color.WHITE,
            binding.color2 to android.graphics.Color.parseColor("#FFE4E4"),
            binding.color3 to android.graphics.Color.parseColor("#E4F0FF"),
            binding.color4 to android.graphics.Color.parseColor("#E4FFE9"),
            binding.color5 to android.graphics.Color.parseColor("#FFF8E4"),
            binding.color6 to android.graphics.Color.parseColor("#F3E4FF")
        )

        colors.forEach { (view, color) ->
            view.setOnClickListener {
                selectedColor = color
                binding.root.setBackgroundColor(color)
            }
        }

        binding.root.setBackgroundColor(selectedColor)
    }

    private fun setupToolbar() {
        refreshToolbarUI()
        binding.btnList.alpha = 0.4f

        binding.btnColor.setOnClickListener {
            binding.colorPickerRow.visibility =
                if (binding.colorPickerRow.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        binding.btnBold.setOnClickListener {
            applyOrRemoveStyle(android.graphics.Typeface.BOLD)
        }

        binding.btnItalic.setOnClickListener {
            applyOrRemoveStyle(android.graphics.Typeface.ITALIC)
        }

        binding.btnFontSize.setOnClickListener {
            val nextSize = when (typingFontSize) {
                16f  -> 18f
                18f  -> 24f
                24f  -> 14f
                else -> 16f
            }
            applyOrRemoveFontSize(nextSize)
        }

        binding.btnList.setOnClickListener {
            binding.listPickerRow.visibility =
                if (binding.listPickerRow.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        binding.listBullet.setOnClickListener {
            listPrefix = "• "
            binding.btnList.alpha = 1f
            binding.listPickerRow.visibility = View.GONE
        }

        binding.listCheckbox.setOnClickListener {
            listPrefix = "☐ "
            binding.btnList.alpha = 1f
            binding.listPickerRow.visibility = View.GONE
        }

        binding.listDash.setOnClickListener {
            listPrefix = "— "
            binding.btnList.alpha = 1f
            binding.listPickerRow.visibility = View.GONE
        }

        binding.listNone.setOnClickListener {
            listPrefix = ""
            binding.btnList.alpha = 0.4f
            binding.listPickerRow.visibility = View.GONE
        }

        binding.btnUndo.setOnClickListener { undo() }
        binding.btnRedo.setOnClickListener { redo() }
        updateUndoRedoButtons()
    }

    private fun pushSnapshot() {
        if (isRestoringSnapshot) return
        val editable = binding.etEditorContent.text ?: return
        val snapshot = EditorSnapshot(
            json = SpanSerializer.toJson(editable as Spannable),
            cursorStart = binding.etEditorContent.selectionStart,
            cursorEnd = binding.etEditorContent.selectionEnd
        )
        // Don't push a duplicate of the top
        if (undoStack.lastOrNull()?.json == snapshot.json) return
        undoStack.addLast(snapshot)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        updateUndoRedoButtons()
    }

    private fun undo() {
        if (undoStack.isEmpty()) return
        val current = EditorSnapshot(
            json = SpanSerializer.toJson(binding.etEditorContent.text as Spannable),
            cursorStart = binding.etEditorContent.selectionStart,
            cursorEnd = binding.etEditorContent.selectionEnd
        )
        redoStack.addLast(current)
        restoreSnapshot(undoStack.removeLast())
        updateUndoRedoButtons()
    }

    private fun redo() {
        if (redoStack.isEmpty()) return
        val current = EditorSnapshot(
            json = SpanSerializer.toJson(binding.etEditorContent.text as Spannable),
            cursorStart = binding.etEditorContent.selectionStart,
            cursorEnd = binding.etEditorContent.selectionEnd
        )
        undoStack.addLast(current)
        restoreSnapshot(redoStack.removeLast())
        updateUndoRedoButtons()
    }

    private fun restoreSnapshot(snapshot: EditorSnapshot) {
        isRestoringSnapshot = true
        isWatcherEnabled = false
        binding.etEditorContent.setText(
            SpanSerializer.fromJson(snapshot.json),
            android.widget.TextView.BufferType.SPANNABLE
        )
        val len = binding.etEditorContent.text?.length ?: 0
        val safeStart = snapshot.cursorStart.coerceIn(0, len)
        val safeEnd = snapshot.cursorEnd.coerceIn(0, len)
        binding.etEditorContent.setSelection(safeStart, safeEnd)
        isWatcherEnabled = true
        isRestoringSnapshot = false
        updateToolbarStateForSelection(safeStart, safeEnd)
    }

    private fun updateUndoRedoButtons() {
        binding.btnUndo.alpha = if (undoStack.isNotEmpty()) 1f else 0.3f
        binding.btnRedo.alpha = if (redoStack.isNotEmpty()) 1f else 0.3f
    }
}
