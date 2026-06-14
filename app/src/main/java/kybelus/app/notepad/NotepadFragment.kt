package kybelus.app.notepad

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import kybelus.app.R
import kybelus.app.databinding.FragmentNotepadBinding
import kybelus.app.notepad.tag.Tag

class NotepadFragment : Fragment() {

    private lateinit var binding: FragmentNotepadBinding
    private val viewModel: NoteViewModel by activityViewModels()
    private lateinit var noteAdapter: NoteAdapter
    private var currentTagId = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotepadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        setupBottomActionBar()
        observeNotes()
        observetags()
        (binding.searchView as SearchView).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchNotes(newText ?: "")
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            emptyList(),
            onNoteClick = { note ->
                if (!noteAdapter.isSelectionMode) {
                    openNoteEditor(note)
                }
            },
            onNoteLongClick = { note ->
                enterSelectionMode(note)
            },
            onPinClick = { note -> viewModel.pinNote(note) }
        )
        binding.recyclerViewNotes.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewNotes.adapter = noteAdapter
    }

    private fun enterSelectionMode(note: Note) {
        noteAdapter.isSelectionMode = true
        noteAdapter.selectedNoteIds.add(note.id)
        noteAdapter.notifyDataSetChanged()
        binding.bottomActionBar.visibility = View.VISIBLE
        binding.fabAddNote.visibility = View.GONE
    }

    private fun exitSelectionMode() {
        noteAdapter.clearSelection()
        binding.bottomActionBar.visibility = View.GONE
        binding.fabAddNote.visibility = View.VISIBLE
    }

    private fun getSelectedNotes(): List<Note> {
        return viewModel.notes.value
            ?.filter { noteAdapter.selectedNoteIds.contains(it.id) }
            ?: emptyList()
    }

    private fun setupBottomActionBar() {
        binding.btnBulkPin.setOnClickListener {
            val selected = getSelectedNotes()
            selected.forEach { note -> viewModel.pinNote(note) }
            exitSelectionMode()
        }

        binding.btnBulkDelete.setOnClickListener {
            val selected = getSelectedNotes()
            val count = selected.size
            AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
                .setTitle("Delete Notes")
                .setMessage("Are you sure you want to delete $count note(s)?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteNotes(selected)
                    exitSelectionMode()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnBulkTag.setOnClickListener {
            if (currentTagId != -1) {
                // In a Tag view — remove notes from Tag
                val selected = getSelectedNotes()
                val updated = selected.map { it.copy(tagId = -1) }
                viewModel.updateNotes(updated)
                exitSelectionMode()
            } else {
                // In "All" view — assign Tag as before
                val selected = getSelectedNotes()
                val tags = viewModel.getAllTags()
                if (tags.isEmpty()) {
                    AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
                        .setTitle("No Tags")
                        .setMessage("Please create a tag first!")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setOnClickListener
                }
                val TagNames = tags.map { it.name }.toTypedArray()
                AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
                    .setTitle("Assign Tag")
                    .setItems(TagNames) { _, which ->
                        val tag = tags[which]
                        val updated = selected.map { note -> note.copy(tagId = tag.id) }
                        viewModel.updateNotes(updated)
                        exitSelectionMode()
                    }
                    .show()
            }
        }

        binding.btnDone.setOnClickListener {
            exitSelectionMode()
        }
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            openNoteEditor(null)
        }
    }

    private fun observeNotes() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.updateNotes(notes)
            if (notes.isEmpty()) {
                binding.tvEmptyNotes.visibility = View.VISIBLE
                binding.recyclerViewNotes.visibility = View.GONE
            } else {
                binding.tvEmptyNotes.visibility = View.GONE
                binding.recyclerViewNotes.visibility = View.VISIBLE
            }
        }
    }

    private fun observetags() {
        viewModel.tags.observe(viewLifecycleOwner) { tags ->
            setupTagChips(tags)
        }
    }

    private fun setupTagChips(tags: List<Tag>) {
        binding.TagFilterRow.removeAllViews()

        addChip("All", -1, currentTagId == -1)

        tags.forEach { Tag ->
            addChip(Tag.name, Tag.id, currentTagId == Tag.id)
        }

        val addChip = TextView(requireContext()).apply {
            text = " + "
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#6650A4"))
            background = requireContext().getDrawable(R.drawable.bg_chip_unselected)
            setPadding(32, 16, 32, 16)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(8, 0, 8, 0) }
            setOnClickListener { showAddTagDialog() }
        }
        binding.TagFilterRow.addView(addChip)
    }

    private fun addChip(label: String, tagId: Int, isSelected: Boolean) {
        val chip = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(
                if (isSelected) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#6650A4")
            )
            background = requireContext().getDrawable(
                if (isSelected) R.drawable.bg_chip_selected
                else R.drawable.bg_chip_unselected
            )
            setPadding(32, 16, 32, 16)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(8, 0, 8, 0) }
            setOnClickListener {
                currentTagId = tagId
                viewModel.filterByTag(tagId)
                setupTagChips(viewModel.getAllTags())
            }
            if (tagId != -1) {
                setOnLongClickListener {
                    AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
                        .setTitle("Delete Tag")
                        .setMessage("Delete \"$label\" tag?")
                        .setPositiveButton("Delete") { _, _ ->
                            val tag = viewModel.getAllTags().find { it.id == tagId }
                            tag?.let { viewModel.deleteTag(it) }
                            if (currentTagId == tagId) {
                                currentTagId = -1
                                viewModel.filterByTag(-1)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
        }
        binding.TagFilterRow.addView(chip)
    }

    private fun showAddTagDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Tag name"
            setHintTextColor(android.graphics.Color.parseColor("#9E8BB5"))
            setTextColor(android.graphics.Color.parseColor("#1C1B1F"))
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext(), R.style.CenteredDialog)
            .setTitle("New Tag")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    viewModel.addTag(Tag(name = name))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openNoteEditor(note: Note?) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragmentContainer, NoteEditorFragment(note) {
                viewModel.notes.value?.let { noteAdapter.updateNotes(it) }
            })
            .addToBackStack(null)
            .commit()
    }
}