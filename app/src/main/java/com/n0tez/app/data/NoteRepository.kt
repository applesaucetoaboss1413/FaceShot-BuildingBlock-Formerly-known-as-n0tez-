package com.n0tez.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class NoteRepository(private val context: Context) {

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SHRED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            sharedPrefs
        }
    }

    fun getAllNotes(): List<Note> {
        val json = sharedPrefs.getString(KEY_NOTES, "[]") ?: "[]"
        return parseNotesFromJson(json)
    }

    fun getActiveNotes(): List<Note> {
        return getAllNotes().filter { !it.isDeleted }
    }

    fun getPinnedNotes(): List<Note> {
        return getAllNotes().filter { it.isPinned && !it.isDeleted }
    }

    fun saveNote(note: Note) {
        val notes = getAllNotes().toMutableList()
        val index = notes.indexOfFirst { it.id == note.id }
        
        note.updatedAt = System.currentTimeMillis()
        
        if (index >= 0) {
            notes[index] = note
        } else {
            notes.add(note)
        }
        saveNotesToPrefs(notes)
    }

    fun deleteNote(noteId: String) {
        val notes = getAllNotes().toMutableList()
        val note = notes.find { it.id == noteId }
        note?.let {
            it.isDeleted = true
            saveNotesToPrefs(notes)
        }
    }

    fun permanentlyDeleteNote(noteId: String) {
        val notes = getAllNotes().toMutableList()
        notes.removeIf { it.id == noteId }
        saveNotesToPrefs(notes)
    }

    fun shredNote(noteId: String) {
        val notes = getAllNotes().toMutableList()
        val note = notes.find { it.id == noteId }
        
        note?.let { noteToShred ->
            // 1. Encrypt current content
            val encryptedContent = securelyEncryptForDeletion(noteToShred.content)
            
            // 2. Overwrite 3 times with random data
            val random = SecureRandom()
            val overwriteData = ByteArray(noteToShred.content.length * 3)
            repeat(3) {
                random.nextBytes(overwriteData)
                noteToShred.content = Base64.encodeToString(overwriteData, Base64.DEFAULT)
            }
            
            // 3. Clear content
            noteToShred.content = ""
            noteToShred.title = ""
            
            // 4. Remove from list
            notes.removeIf { n -> n.id == noteId }
            saveNotesToPrefs(notes)
            
            // 5. Store encrypted content briefly then remove (shred logic?)
            securePrefs.edit().putString("shredded_$noteId", encryptedContent).apply()
            securePrefs.edit().remove("shredded_$noteId").apply()
        }
    }

    fun pinNote(noteId: String, pinned: Boolean) {
        val notes = getAllNotes().toMutableList()
        val note = notes.find { it.id == noteId }
        note?.let {
            it.isPinned = pinned
            saveNotesToPrefs(notes)
        }
    }

    fun getCurrentNote(): Note? {
        val json = sharedPrefs.getString(KEY_CURRENT_NOTE, null)
        return if (json != null) parseNoteFromJson(json) else null
    }

    fun setCurrentNote(note: Note?) {
        if (note != null) {
            sharedPrefs.edit().putString(KEY_CURRENT_NOTE, noteToJson(note)).apply()
        } else {
            sharedPrefs.edit().remove(KEY_CURRENT_NOTE).apply()
        }
    }

    private fun saveNotesToPrefs(notes: List<Note>) {
        val jsonArray = JSONArray()
        notes.forEach { jsonArray.put(noteToJsonObject(it)) }
        sharedPrefs.edit().putString(KEY_NOTES, jsonArray.toString()).apply()
    }

    private fun parseNotesFromJson(json: String): List<Note> {
        val notes = ArrayList<Note>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                notes.add(parseNoteFromJsonObject(jsonArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return notes.sortedByDescending { it.updatedAt }
    }

    private fun parseNoteFromJson(json: String): Note? {
        return try {
            parseNoteFromJsonObject(JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNoteFromJsonObject(json: JSONObject): Note {
        return Note(
            id = json.optString("id"),
            title = json.optString("title"),
            content = json.optString("content"),
            createdAt = json.optLong("createdAt"),
            updatedAt = json.optLong("updatedAt"),
            isPinned = json.optBoolean("isPinned"),
            isDeleted = json.optBoolean("isDeleted")
            // attachments ignored as per decompiled code
        )
    }

    private fun noteToJson(note: Note): String {
        return noteToJsonObject(note).toString()
    }

    private fun noteToJsonObject(note: Note): JSONObject {
        return JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("content", note.content)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)
            put("isPinned", note.isPinned)
            put("isDeleted", note.isDeleted)
        }
    }

    private fun securelyEncryptForDeletion(content: String): String {
        return try {
            val random = SecureRandom()
            val keyBytes = ByteArray(32)
            random.nextBytes(keyBytes)
            val key = SecretKeySpec(keyBytes, "AES")
            
            val iv = ByteArray(12)
            random.nextBytes(iv)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            
            val encrypted = cipher.doFinal(content.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            // Fallback to random garbage if crypto fails
            val randomBytes = ByteArray(content.length)
            SecureRandom().nextBytes(randomBytes)
            Base64.encodeToString(randomBytes, Base64.DEFAULT)
        }
    }

    companion object {
        private const val KEY_NOTES = "notes_list"
        private const val KEY_CURRENT_NOTE = "current_note"
        private const val PREFS_NAME = "faceshot_buildingblock_notes"
        private const val SHRED_PREFS_NAME = "faceshot_buildingblock_shred"
    }
}
