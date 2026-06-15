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
            binding.tvEditorTitle.text = "✏️ Edit Note"
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isWatcherEnabled || s == null) return
                val end   = binding.etEditorContent.selectionEnd
                val start = end - 1
                if (start < 0) return

                if (listPrefix.isNotEmpty() && s.isNotEmpty() && end > 0) {
                    if (s[start] == '\n') {
                        isWatcherEnabled = false
                        s.insert(end, listPrefix)
                        isWatcherEnabled = true
                    }
                }

                if (typingBold) {
                    s.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (typingItalic) {
                    s.setSpan(
                        StyleSpan(android.graphics.Typeface.ITALIC),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (typingFontSize != 16f) {
                    s.setSpan(
                        AbsoluteSizeSpan(typingFontSize.toInt(), true),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        })
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
            if (style == android.graphics.Typeface.BOLD) {
                typingBold   = !typingBold
                binding.btnBold.alpha = if (typingBold) 1f else 0.4f
            } else {
                typingItalic = !typingItalic
                binding.btnItalic.alpha = if (typingItalic) 1f else 0.4f
            }
            return
        }

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
        if (style == android.graphics.Typeface.BOLD) {
            binding.btnBold.alpha = if (typingBold) 1f else 0.4f
        } else {
            binding.btnItalic.alpha = if (typingItalic) 1f else 0.4f
        }
    }

    private fun applyOrRemoveFontSize(size: Float) {
        val editable  = binding.etEditorContent.text ?: return
        val selStart  = binding.etEditorContent.selectionStart
        val selEnd    = binding.etEditorContent.selectionEnd

        if (selStart == selEnd) {
            typingFontSize = when (typingFontSize) {
                16f  -> 18f
                18f  -> 24f
                24f  -> 14f
                else -> 16f
            }
            binding.btnFontSize.text = when (typingFontSize) {
                14f  -> "A-"
                18f  -> "A+"
                24f  -> "A++"
                else -> "A"
            }
            binding.btnFontSize.alpha = if (typingFontSize != 16f) 1f else 0.4f
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
        binding.btnBold.alpha     = 0.4f
        binding.btnItalic.alpha   = 0.4f
        binding.btnFontSize.alpha = 0.4f
        binding.btnList.alpha     = 0.4f

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
    }
}
