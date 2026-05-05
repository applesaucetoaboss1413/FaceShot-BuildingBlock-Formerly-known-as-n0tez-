package com.n0tez.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.ui.compose.FaceshotTheme
import com.n0tez.app.ui.compose.FuturisticScreen
import com.n0tez.app.ui.compose.GlassPanel
import com.n0tez.app.ui.compose.PrimaryButton
import com.n0tez.app.ui.compose.SecondaryButton
import com.n0tez.app.ui.compose.SectionHeader
import com.n0tez.app.ui.compose.StatusPillRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var noteRepository: NoteRepository
    private var currentNote: Note? = null
    private var noteId: String? = null
    private var autoSaveJob: Job? = null
    private val autoSaveScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var editorValue by mutableStateOf(TextFieldValue(""))
    private var transparencyLevel by mutableStateOf(70f)
    private var lastSavedAt by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteRepository = NoteRepository(this)
        noteId = intent.getStringExtra("NOTE_ID")
        transparencyLevel = loadTransparencyPreference().toFloat()
        loadNote()
        setContent {
            FaceshotTheme {
                NoteEditorScreen()
            }
        }
    }

    private fun loadNote() {
        if (noteId != null) {
            val notes = noteRepository.getAllNotes()
            currentNote = notes.find { it.id == noteId }
            currentNote?.let {
                editorValue = TextFieldValue(it.content)
                lastSavedAt = it.updatedAt
            }
        }
    }

    private fun onEditorChanged(value: TextFieldValue) {
        editorValue = value
        autoSaveJob?.cancel()
        autoSaveJob = autoSaveScope.launch {
            delay(2000)
            saveNote(finishAfter = false, showToast = false)
        }
    }

    private fun saveNote(finishAfter: Boolean, showToast: Boolean = true) {
        val content = editorValue.text
        if (content.isBlank() && currentNote == null) {
            if (finishAfter) {
                Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (currentNote == null) {
            val title = extractTitle(content)
            currentNote = Note(
                content = content,
                title = title
            )
            noteRepository.saveNote(currentNote!!)
            noteId = currentNote!!.id
        } else {
            currentNote!!.content = content
            currentNote!!.title = extractTitle(content)
            currentNote!!.updatedAt = System.currentTimeMillis()
            noteRepository.saveNote(currentNote!!)
        }

        lastSavedAt = currentNote?.updatedAt ?: System.currentTimeMillis()

        if (finishAfter) {
            if (showToast) {
                Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
            }
            finish()
        } else if (showToast) {
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractTitle(content: String): String {
        val lines = content.lines()
        return if (lines.isNotEmpty()) lines[0].take(50) else "Untitled"
    }

    private fun shareNote() {
        val content = editorValue.text
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
        val content = editorValue.text
        if (content.isNotBlank()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Note", content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pasteContent() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pastedText = clip.getItemAt(0).coerceToText(this).toString()
            val start = editorValue.selection.start.coerceAtLeast(0)
            val end = editorValue.selection.end.coerceAtLeast(0)
            val replaced = editorValue.text.replaceRange(
                start = minOf(start, end),
                endIndex = maxOf(start, end),
                replacement = pastedText,
            )
            val newCursor = minOf(start, end) + pastedText.length
            onEditorChanged(
                editorValue.copy(
                    text = replaced,
                    selection = androidx.compose.ui.text.TextRange(newCursor),
                ),
            )
        }
    }

    private fun loadTransparencyPreference(): Int {
        return getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .getInt("transparency_level", 70)
    }

    private fun saveTransparencyPreference(level: Int) {
        getSharedPreferences("n0tez_prefs", MODE_PRIVATE).edit()
            .putInt("transparency_level", level)
            .apply()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        autoSaveJob?.cancel()
        autoSaveScope.cancel()
    }

    @Composable
    private fun NoteEditorScreen() {
        val wordCount = editorValue.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val characterCount = editorValue.text.length
        val title = if (noteId == null) getString(R.string.new_note) else getString(R.string.edit_note)
        val savedLabel = if (lastSavedAt > 0L) {
            "Saved ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastSavedAt))}"
        } else {
            "Autosave ready"
        }

        FuturisticScreen(
            title = title,
            onBack = { finish() },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroSection(savedLabel = savedLabel)
                StatusPillRow(
                    statuses = listOf(
                        "Words $wordCount" to (wordCount > 0),
                        "Chars $characterCount" to (characterCount > 0),
                        "Transparency ${transparencyLevel.toInt()}%" to true,
                    ),
                )
                GlassPanel {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SectionHeader(
                            title = "Editor Controls",
                            subtitle = "Tune readability, move text between apps, and save when you are ready.",
                        )
                        Text(
                            text = "Transparency",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Slider(
                            value = transparencyLevel,
                            onValueChange = {
                                transparencyLevel = it
                                saveTransparencyPreference(it.toInt())
                            },
                            valueRange = 35f..100f,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PrimaryButton(
                                text = getString(R.string.save_note),
                                onClick = { saveNote(finishAfter = true) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = getString(R.string.share_note),
                                onClick = ::shareNote,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SecondaryButton(
                                text = getString(R.string.copy),
                                onClick = ::copyNoteContent,
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = getString(R.string.paste_note),
                                onClick = ::pasteContent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                TextField(
                    value = editorValue,
                    onValueChange = ::onEditorChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = {
                        Text("Start writing...")
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = transparencyLevel / 100f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = transparencyLevel / 100f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }

    @Composable
    private fun HeroSection(savedLabel: String) {
        GlassPanel {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Focused writing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "A cleaner editor with autosave, sharing, and clipboard tools built in.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = savedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
