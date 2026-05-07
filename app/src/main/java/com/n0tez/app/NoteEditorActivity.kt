package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.ui.components.AppScreen
import com.n0tez.app.ui.components.GlassPanel
import com.n0tez.app.ui.components.HeroPanel
import com.n0tez.app.ui.components.MetricChip
import com.n0tez.app.ui.theme.N0tezTheme
import kotlinx.coroutines.*
import java.util.*

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var noteRepository: NoteRepository
    private var currentNote: Note? = null
    private var noteId: String? = null
    private var autoSaveJob: Job? = null
    private val autoSaveScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var noteContent by mutableStateOf(TextFieldValue(""))
    private var transparencyLevel by mutableFloatStateOf(70f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteRepository = NoteRepository(this)
        noteId = intent.getStringExtra("NOTE_ID")
        loadTransparencyPreference()
        loadNote()
        setContent {
            N0tezTheme {
                NoteEditorScreen(
                    title = if (noteId == null) "New Note" else "Edit Note",
                    noteValue = noteContent,
                    transparency = transparencyLevel,
                    onBack = ::finish,
                    onNoteChange = {
                        noteContent = it
                        scheduleAutoSave()
                    },
                    onTransparencyChange = {
                        transparencyLevel = it
                        saveTransparencyPreference(it.toInt())
                    },
                    onSave = { saveNote(finishAfter = true) },
                    onShare = ::shareNote,
                    onCopy = ::copyNoteContent,
                    onPaste = ::pasteContent
                )
            }
        }
    }

    private fun loadTransparencyPreference() {
        transparencyLevel = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .getInt("transparency_level", 70)
            .toFloat()
    }

    private fun loadNote() {
        if (noteId != null) {
            val notes = noteRepository.getAllNotes()
            currentNote = notes.find { it.id == noteId }
            currentNote?.let {
                noteContent = TextFieldValue(it.content)
            }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = autoSaveScope.launch {
            delay(2000)
            saveNote(finishAfter = false)
        }
    }

    private fun saveNote(finishAfter: Boolean) {
        val content = noteContent.text
        if (content.isBlank() && currentNote == null) {
            if (finishAfter) {
                Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        if (currentNote == null) {
            // Create new
            val title = extractTitle(content)
            currentNote = Note(
                content = content,
                title = title
            )
            noteRepository.saveNote(currentNote!!)
            noteId = currentNote!!.id
        } else {
            // Update
            currentNote!!.content = content
            currentNote!!.title = extractTitle(content)
            currentNote!!.updatedAt = System.currentTimeMillis()
            noteRepository.saveNote(currentNote!!)
        }
        
        if (finishAfter) {
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun extractTitle(content: String): String {
        val lines = content.lines()
        return if (lines.isNotEmpty()) lines[0].take(50) else "Untitled"
    }

    private fun shareNote() {
        val content = noteContent.text
        if (content.isNotBlank()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, "Note from n0tez")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Note"))
        }
    }

    private fun copyNoteContent() {
        val content = noteContent.text
        if (content.isNotBlank()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Note", content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pasteContent() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pastedText = clip.getItemAt(0).text.toString()
            val start = noteContent.selection.start.coerceAtLeast(0)
            val end = noteContent.selection.end.coerceAtLeast(0)
            val min = minOf(start, end)
            val max = maxOf(start, end)
            val merged = buildString {
                append(noteContent.text.substring(0, min))
                append(pastedText)
                append(noteContent.text.substring(max))
            }
            val newCursor = min + pastedText.length
            noteContent = TextFieldValue(
                text = merged,
                selection = androidx.compose.ui.text.TextRange(newCursor)
            )
            scheduleAutoSave()
        }
    }

    private fun saveTransparencyPreference(level: Int) {
        getSharedPreferences("n0tez_prefs", MODE_PRIVATE).edit()
            .putInt("transparency_level", level)
            .apply()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSaveJob?.cancel()
        autoSaveScope.cancel()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteEditorScreen(
    title: String,
    noteValue: TextFieldValue,
    transparency: Float,
    onBack: () -> Unit,
    onNoteChange: (TextFieldValue) -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit
) {
    val wordCount = noteValue.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val lineCount = noteValue.text.lines().size.coerceAtLeast(1)

    AppScreen(
        title = title,
        subtitle = "Compose writing surface",
        navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
        onNavigationClick = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroPanel(
                    eyebrow = "Editor",
                    title = "A calmer writing surface with better spacing.",
                    description = "The note editor now uses a clearer hierarchy, live metrics, and modern action controls without changing your save logic.",
                    metrics = {
                        MetricChip("Words", wordCount.toString(), Icons.Rounded.Edit)
                        MetricChip("Lines", lineCount.toString(), Icons.Rounded.ContentCopy)
                        MetricChip("Focus", "${transparency.toInt()}%", Icons.Rounded.ContentPaste)
                    }
                )
            }
            item {
                GlassPanel {
                    Text(
                        text = "Canvas transparency",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = transparency,
                        onValueChange = onTransparencyChange,
                        valueRange = 25f..100f
                    )
                    Text(
                        text = "${transparency.toInt()}% visible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                GlassPanel {
                    OutlinedTextField(
                        value = noteValue,
                        onValueChange = onNoteChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = transparency / 100f)
                        ),
                        placeholder = {
                            Text("Write something clear, sharp, and worth saving.")
                        }
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = onSave) {
                            Icon(Icons.Rounded.Save, contentDescription = null)
                            Text(" Save")
                        }
                        FilledTonalButton(onClick = onShare) {
                            Icon(Icons.Rounded.IosShare, contentDescription = null)
                            Text(" Share")
                        }
                        FilledTonalButton(onClick = onCopy) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                            Text(" Copy")
                        }
                        FilledTonalButton(onClick = onPaste) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null)
                            Text(" Paste")
                        }
                    }
                }
            }
        }
    }
}
