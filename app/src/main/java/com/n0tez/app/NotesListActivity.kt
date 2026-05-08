package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.ui.compose.FaceshotTheme
import com.n0tez.app.ui.compose.FuturisticScreen
import com.n0tez.app.ui.compose.GlassPanel
import com.n0tez.app.ui.compose.HeroCard
import com.n0tez.app.ui.compose.MetricCard
import com.n0tez.app.ui.compose.NotesList
import com.n0tez.app.ui.compose.PrimaryButton
import com.n0tez.app.ui.compose.SectionHeader
import com.n0tez.app.ui.compose.StatusPillRow

class NotesListActivity : AppCompatActivity() {

    private lateinit var noteRepository: NoteRepository
    private var isPinVerified = false
    private val notesState = mutableStateListOf<Note>()
    private var dialogState by mutableStateOf<NoteDialogState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PinLockActivity.isPinEnabled(this) && !isPinVerified) {
            val intent = Intent(this, PinLockActivity::class.java)
            intent.putExtra("SET_PIN", false)
            startActivityForResult(intent, REQUEST_PIN_VERIFICATION)
            return
        }
        
        setupActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PIN_VERIFICATION) {
            if (resultCode == RESULT_OK) {
                isPinVerified = true
                setupActivity()
            } else {
                finish()
            }
        }
    }

    private fun setupActivity() {
        noteRepository = NoteRepository(this)
        setContent {
            FaceshotTheme {
                NotesScreen()
            }
        }
        loadNotes()
        setContent {
            N0tezTheme {
                NotesListScreen(
                    notes = notes,
                    pendingDeleteNote = pendingDeleteNote,
                    pendingShredNote = pendingShredNote,
                    onBack = ::finish,
                    onCreateNote = {
                        startActivity(Intent(this, NoteEditorActivity::class.java))
                    },
                    onOpenNote = ::openNote,
                    onPinNote = ::togglePin,
                    onDeleteNote = {
                        pendingDeleteNote = it
                    },
                    onShredNote = {
                        pendingShredNote = it
                    },
                    onDismissDelete = {
                        pendingDeleteNote = null
                    },
                    onDismissShred = {
                        pendingShredNote = null
                    },
                    onConfirmDelete = {
                        pendingDeleteNote?.let { noteRepository.deleteNote(it.id) }
                        pendingDeleteNote = null
                        loadNotes()
                    },
                    onConfirmShred = {
                        pendingShredNote?.let { noteRepository.shredNote(it.id) }
                        pendingShredNote = null
                        loadNotes()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::noteRepository.isInitialized) {
            loadNotes()
        }
    }

    private fun loadNotes() {
        if (!::noteRepository.isInitialized) return
        notesState.clear()
        notesState.addAll(
            noteRepository.getActiveNotes().sortedWith(
                compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
            )
        )
    }

    private fun openNote(note: Note) {
        val intent = Intent(this, NoteEditorActivity::class.java)
        intent.putExtra("NOTE_ID", note.id)
        startActivity(intent)
    }

    private fun togglePin(note: Note) {
        noteRepository.pinNote(note.id, !note.isPinned)
        loadNotes()
        Toast.makeText(
            this,
            if (note.isPinned) getString(R.string.unpin_note) else getString(R.string.pin_note),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun confirmDelete(note: Note) {
        dialogState = NoteDialogState(
            title = "Delete note?",
            message = "This removes the note from the active list while keeping the rest of your workspace intact.",
            confirmLabel = "Delete",
            onConfirm = {
                noteRepository.deleteNote(note.id)
                loadNotes()
                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show()
            },
        )
    }

    private fun confirmShred(note: Note) {
        dialogState = NoteDialogState(
            title = "Shred note?",
            message = "This securely destroys the note and removes it permanently. This action cannot be undone.",
            confirmLabel = "Shred",
            onConfirm = {
                noteRepository.shredNote(note.id)
                loadNotes()
            },
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val REQUEST_PIN_VERIFICATION = 1001
    }
}

    @Composable
    private fun NotesScreen() {
        val notes = remember { notesState }
        val pinnedCount = notes.count { it.isPinned }
        val latestUpdate = notes.firstOrNull()?.let { "Live" } ?: "Empty"

        FuturisticScreen(
            title = getString(R.string.my_notes),
            onBack = { finish() },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    HeroCard(
                        eyebrow = "Secure Notes",
                        title = "Your notes are clearer, faster, and easier to manage.",
                        description = "Pinned notes stay on top, destructive actions are isolated, and the layout now reads like a premium workspace.",
                    )
                }
                item {
                    StatusPillRow(
                        statuses = listOf(
                            "PIN Active" to PinLockActivity.isPinEnabled(this@NotesListActivity),
                            "Pinned Notes" to (pinnedCount > 0),
                            "Workspace Active" to notes.isNotEmpty(),
                        ),
                    )
                }
                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        MetricCard(
                            label = "Active Notes",
                            value = notes.size.toString(),
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Pinned",
                            value = pinnedCount.toString(),
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    GlassPanel {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SectionHeader(
                                title = "Quick Actions",
                                subtitle = "Move from capture to editing without extra taps.",
                            )
                            PrimaryButton(
                                text = getString(R.string.new_note),
                                onClick = { startActivity(Intent(this@NotesListActivity, NoteEditorActivity::class.java)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "Workspace state: $latestUpdate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    SectionHeader(
                        title = "All Notes",
                        subtitle = "Recent updates stay visible and pinned notes lead the queue.",
                    )
                }
                item {
                    NotesList(
                        notes = notes,
                        onNoteClick = ::openNote,
                        onPinClick = ::togglePin,
                        onDeleteClick = ::confirmDelete,
                        onShredClick = ::confirmShred,
                        emptyTitle = "No notes yet",
                        emptySubtitle = "Create your first note to start building a clean, secure workspace.",
                    )
                }
            }
        }

        dialogState?.let { dialog ->
            AlertDialog(
                onDismissRequest = { dialogState = null },
                title = { Text(dialog.title) },
                text = { Text(dialog.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog.onConfirm()
                            dialogState = null
                        },
                    ) {
                        Text(dialog.confirmLabel)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogState = null }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }

    private data class NoteDialogState(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val onConfirm: () -> Unit,
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
