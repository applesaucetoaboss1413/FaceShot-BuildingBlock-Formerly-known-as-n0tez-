package com.n0tez.app.data

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isPinned: Boolean = false,
    var isDeleted: Boolean = false,
    var attachments: List<MediaAttachment> = emptyList()
) {
    fun getDisplayTitle(): String {
        return if (title.isNotBlank()) title else if (content.isNotBlank()) content.take(30) else "Untitled Note"
    }

    fun getPreviewText(): String {
        return if (content.isNotBlank()) content.take(100).replace("\n", " ") else "No additional text"
    }
}