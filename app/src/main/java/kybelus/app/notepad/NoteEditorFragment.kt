package kybelus.app.notepad

import android.os.Bundle
import android.text.Editable
import android.text.Html
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

    private var fontSize = 16f
    private var isBold = false
    private var isItalic = false
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
                Html.fromHtml(it.content, Html.FROM_HTML_MODE_COMPACT),
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
            val content = Html.toHtml(
                binding.etEditorContent.text,
                Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
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
                val end = binding.etEditorContent.selectionEnd
                val start = end - 1
                if (start < 0) return

                // Add list prefix on new line
                if (listPrefix.isNotEmpty() && s.isNotEmpty() && end > 0) {
                    val justTyped = s[start]
                    if (justTyped == '\n') {
                        isWatcherEnabled = false
                        s.insert(end, listPrefix)
                        isWatcherEnabled = true
                    }
                }

                if (isBold) {
                    s.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (isItalic) {
                    s.setSpan(
                        StyleSpan(android.graphics.Typeface.ITALIC),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (fontSize != 16f) {
                    s.setSpan(
                        AbsoluteSizeSpan(fontSize.toInt(), true),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        })
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
        binding.btnBold.alpha = 0.4f
        binding.btnItalic.alpha = 0.4f
        binding.btnFontSize.alpha = 0.4f
        binding.btnList.alpha = 0.4f

        binding.btnColor.setOnClickListener {
            binding.colorPickerRow.visibility = if (binding.colorPickerRow.visibility == View.GONE)
                View.VISIBLE else View.GONE
        }

        binding.btnBold.setOnClickListener {
            isBold = !isBold
            binding.btnBold.alpha = if (isBold) 1f else 0.4f
        }

        binding.btnItalic.setOnClickListener {
            isItalic = !isItalic
            binding.btnItalic.alpha = if (isItalic) 1f else 0.4f
        }

        binding.btnFontSize.setOnClickListener {
            fontSize = when (fontSize) {
                16f -> 18f
                18f -> 24f
                24f -> 14f
                else -> 16f
            }
            binding.etEditorContent.textSize = fontSize
            binding.btnFontSize.text = when (fontSize) {
                14f -> "A-"
                16f -> "A"
                18f -> "A+"
                else -> "A++"
            }
            binding.btnFontSize.alpha = if (fontSize != 16f) 1f else 0.4f
        }

        binding.btnList.setOnClickListener {
            binding.listPickerRow.visibility = if (binding.listPickerRow.visibility == View.GONE)
                View.VISIBLE else View.GONE
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