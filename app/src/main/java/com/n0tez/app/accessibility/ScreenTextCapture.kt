package com.n0tez.app.accessibility

import android.graphics.Rect

internal data class ScreenTextNode(
    val text: String,
    val bounds: Rect,
    val packageName: CharSequence?,
    val isVisibleToUser: Boolean,
)

internal object ScreenTextCapture {
    fun extractText(
        nodes: List<ScreenTextNode>,
        targetBounds: Rect,
        excludedPackageName: String,
    ): String {
        if (targetBounds.isEmpty) return ""

        return nodes
            .asSequence()
            .filter { it.isVisibleToUser }
            .filter { !it.text.isBlank() }
            .filter { it.packageName?.toString() != excludedPackageName }
            .filter { Rect.intersects(it.bounds, targetBounds) }
            .sortedWith(compareBy<ScreenTextNode> { it.bounds.top }.thenBy { it.bounds.left })
            .map { it.text.trim() }
            .distinct()
            .joinToString(separator = "\n")
            .trim()
    }
}
