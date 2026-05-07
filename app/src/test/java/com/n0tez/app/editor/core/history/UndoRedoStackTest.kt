package com.n0tez.app.editor.core.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoStackTest {
    @Test
    fun pushUndoRedo_tracksStateDeterministically() {
        val stack = UndoRedoStack(initial = 0, maxSize = 3)

        assertEquals(0, stack.current)
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)

        stack.push(1)
        stack.push(2)
        assertEquals(2, stack.current)
        assertTrue(stack.canUndo)
        assertFalse(stack.canRedo)

        assertEquals(1, stack.undo())
        assertEquals(1, stack.current)
        assertTrue(stack.canRedo)

        assertEquals(2, stack.redo())
        assertEquals(2, stack.current)
        assertFalse(stack.canRedo)
    }

    @Test
    fun push_clearsRedoAndRespectsMaxSize() {
        val stack = UndoRedoStack(initial = "A", maxSize = 2)
        stack.push("B")
        stack.push("C")
        stack.push("D")

        assertEquals("D", stack.current)
        assertEquals("C", stack.undo())
        assertEquals("B", stack.undo())
        assertFalse(stack.canUndo)

        stack.push("E")
        assertFalse(stack.canRedo)
    }
}

