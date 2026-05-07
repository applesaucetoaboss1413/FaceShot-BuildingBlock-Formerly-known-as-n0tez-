package com.n0tez.app.data

import java.util.UUID

data class MediaAttachment(
    val id: String = UUID.randomUUID().toString(),
    val type: MediaType,
    val filePath: String,
    val thumbnailPath: String? = null,
    val duration: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var title: String
) {
    fun getFormattedDuration(): String {
        if (duration == null) return ""
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    fun getFormattedSize(): String {
        return if (fileSize < 1024) {
            "$fileSize B"
        } else if (fileSize < 1048576) {
            "${fileSize / 1024} KB"
        } else {
            String.format("%.2f MB", fileSize / 1048576.0)
        }
    }
}