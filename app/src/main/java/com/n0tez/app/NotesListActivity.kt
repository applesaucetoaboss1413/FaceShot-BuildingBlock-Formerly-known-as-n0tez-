package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.ui.components.AppScreen
import com.n0tez.app.ui.components.HeroPanel
import com.n0tez.app.ui.components.MetricChip
import com.n0tez.app.ui.theme.N0tezTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesListActivity : AppCompatActivity() {

    private lateinit var noteRepository: NoteRepository
    private var isPinVerified = false
    private var notes by mutableStateOf<List<Note>>(emptyList())
    private var pendingDeleteNote by mutableStateOf<Note?>(null)
    private var pendingShredNote by mutableStateOf<Note?>(null)

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
        notes = noteRepository.getActiveNotes()
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    private fun openNote(note: Note) {
        val intent = Intent(this, NoteEditorActivity::class.java)
        intent.putExtra("NOTE_ID", note.id)
        startActivity(intent)
    }

    private fun togglePin(note: Note) {
        noteRepository.pinNote(note.id, !note.isPinned)
        loadNotes()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val REQUEST_PIN_VERIFICATION = 1001
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListScreen(
    notes: List<Note>,
    pendingDeleteNote: Note?,
    pendingShredNote: Note?,
    onBack: () -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Note) -> Unit,
    onPinNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onShredNote: (Note) -> Unit,
    onDismissDelete: () -> Unit,
    onDismissShred: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmShred: () -> Unit
) {
    AppScreen(
        title = "Notes Vault",
        subtitle = "Private writing, elevated",
        navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
        onNavigationClick = onBack
    ) { innerPadding ->
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(onClick = onCreateNote) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create note")
                }
            }
        ) { scaffoldPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(scaffoldPadding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HeroPanel(
                        eyebrow = "Vault",
                        title = "Your writing now feels curated and easier to act on.",
                        description = "Pinned notes stay visible, destructive actions are clearly isolated, and the screen now reads like a premium private workspace.",
                        metrics = {
                            MetricChip("Active notes", notes.size.toString(), Icons.Rounded.Visibility)
                            MetricChip("Pinned", notes.count { it.isPinned }.toString(), Icons.Rounded.PushPin)
                            MetricChip("Create flow", "Instant", Icons.Rounded.RocketLaunch)
                        }
                    )
                }
                if (notes.isEmpty()) {
                    item {
                        HeroPanel(
                            eyebrow = "Empty state",
                            title = "No notes yet.",
                            description = "Create your first note to start building a cleaner, safer personal workspace.",
                            metrics = {
                                MetricChip("Privacy", "Local first", Icons.Rounded.Lock)
                                MetricChip("Capture", "Ready", Icons.Rounded.Add)
                            }
                        )
                    }
                } else {
                    items(notes.size) { index ->
                        NoteCard(
                            note = notes[index],
                            onOpenNote = onOpenNote,
                            onPinNote = onPinNote,
                            onDeleteNote = onDeleteNote,
                            onShredNote = onShredNote
                        )
                    }
                }
            }
        }
    }

    if (pendingDeleteNote != null) {
        ConfirmDialog(
            title = "Delete note?",
            message = "The note will move out of the active list and can be cleaned up later.",
            confirmLabel = "Delete",
            onDismiss = onDismissDelete,
            onConfirm = onConfirmDelete
        )
    }

    if (pendingShredNote != null) {
        ConfirmDialog(
            title = "Shred note?",
            message = "This permanently overwrites the content and cannot be undone.",
            confirmLabel = "Shred",
            onDismiss = onDismissShred,
            onConfirm = onConfirmShred
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onOpenNote: (Note) -> Unit,
    onPinNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onShredNote: (Note) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenNote(note) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = note.getDisplayTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(note.updatedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MetricChip(
                    label = if (note.isPinned) "Pinned" else "Unlocked",
                    value = if (note.isPinned) "Vault" else "Draft",
                    icon = if (note.isPinned) Icons.Rounded.PushPin else Icons.Rounded.LockOpen
                )
            }
            Text(
                text = note.getPreviewText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onPinNote(note) }) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Rounded.Lock else Icons.Rounded.PushPin,
                        contentDescription = "Pin note"
                    )
                }
                IconButton(onClick = { onDeleteNote(note) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete note")
                }
                IconButton(onClick = { onShredNote(note) }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Shred note",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
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
