package com.n0tez.app.editor.core.history

/**
 * A small, deterministic undo/redo stack.
 *
 * This is intended to back UI timelines and editor command history in a consistent way across
 * different editor surfaces.
 */
class UndoRedoStack<T>(
    initial: T,
    private val maxSize: Int = 100,
) {
    private val undo = ArrayDeque<T>()
    private val redo = ArrayDeque<T>()

    var current: T = initial
        private set

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    @Synchronized
    fun push(next: T) {
        undo.addLast(current)
        current = next
        redo.clear()
        while (undo.size > maxSize) {
            undo.removeFirst()
        }
    }

    @Synchronized
    fun undo(): T? {
        val prev = undo.removeLastOrNull() ?: return null
        redo.addLast(current)
        current = prev
        return current
    }

    @Synchronized
    fun redo(): T? {
        val next = redo.removeLastOrNull() ?: return null
        undo.addLast(current)
        current = next
        return current
    }

    @Synchronized
    fun clear(newCurrent: T) {
        undo.clear()
        redo.clear()
        current = newCurrent
    }
}

