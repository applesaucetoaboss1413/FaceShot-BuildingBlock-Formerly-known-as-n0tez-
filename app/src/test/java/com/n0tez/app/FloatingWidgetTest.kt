package com.n0tez.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.databinding.FloatingBubbleBinding
import com.n0tez.app.databinding.FloatingNotepadBinding
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class FloatingWidgetTest {

    private lateinit var service: FloatingWidgetService
    private lateinit var mockWindowManager: WindowManager
    private lateinit var mockNoteRepository: NoteRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockWindowManager = mockk(relaxed = true)
        mockNoteRepository = mockk(relaxed = true)

        // Initialize Service
        val controller = Robolectric.buildService(FloatingWidgetService::class.java)
        service = controller.get()
        
        // Inject mocks via reflection BEFORE onCreate
        // We can't inject before onCreate with Robolectric controller easily without a custom ShadowApplication
        // So we let onCreate run.
        
        try {
            // Set Theme to avoid inflation errors
            context.setTheme(com.n0tez.app.R.style.Theme_N0tez)
            controller.create()
        } catch (e: Exception) {
            println("Warning: Service create failed: ${e.message}")
            e.printStackTrace()
        }

        // Swap WindowManager (it was initialized in onCreate with real one, swap it now)
        setPrivateField(service, "windowManager", mockWindowManager)
        
        // Swap NoteRepository
        setPrivateField(service, "noteRepository", mockNoteRepository)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
    
    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    @Test
    fun `test Bubble Open Performance and Logic`() {
        println("Starting Bubble Open Test...")

        // 1. Setup Bubble View
        val mockBubbleView = mockk<FrameLayout>(relaxed = true)
        setPrivateField(service, "floatingBubbleView", mockBubbleView)

        val toggleMethod = service::class.java.getDeclaredMethod("toggleNotepad")
        toggleMethod.isAccessible = true

        val time = measureTimeMillis {
            toggleMethod.invoke(service)
            Shadows.shadowOf(Looper.getMainLooper()).idle() // Execute Handler.post
        }

        println("Bubble Open Time: ${time}ms")
        assertTrue("Open time should be < 200ms", time < 200)

        // Verify Notepad Opened flag
        assertTrue("Notepad should be opened", service.testNotepadOpened)
    }

    @Test
    fun `test Rapid Click Debounce`() {
        println("Testing Rapid Click...")
        service.testNotepadOpened = false
        
        val toggleMethod = service::class.java.getDeclaredMethod("toggleNotepad")
        toggleMethod.isAccessible = true

        // Click 1
        toggleMethod.invoke(service)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(service.testNotepadOpened)
        service.testNotepadOpened = false
        
        // Click 2 (immediately - transitioning is still true due to postDelayed 300ms)
        toggleMethod.invoke(service)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Should NOT open again
        assertTrue("Should not open again", !service.testNotepadOpened)
        println("Rapid click handled: Second click ignored.")
    }
    
    @Test
    fun `test Touch Logic (Click vs Drag)`() {
        service.testNotepadOpened = false
        val mockBubbleView = mockk<FrameLayout>(relaxed = true)
        setPrivateField(service, "floatingBubbleView", mockBubbleView)
        
        val setupMethod = service::class.java.getDeclaredMethod("setupBubbleDragAndTap")
        setupMethod.isAccessible = true
        setupMethod.invoke(service)
        
        val listenerSlot = slot<View.OnTouchListener>()
        verify { mockBubbleView.setOnTouchListener(capture(listenerSlot)) }
        val listener = listenerSlot.captured
        
        // Test CLICK
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        val upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 100f, 100f, 0)
        
        listener.onTouch(mockBubbleView, downEvent)
        listener.onTouch(mockBubbleView, upEvent)
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Execute Handler
        
        assertTrue("Click should open notepad", service.testNotepadOpened)
        println("Normal Click: Verified")
        
        service.testNotepadOpened = false
        
        // Test DRAG
        val downEvent2 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        val moveEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, 200f, 200f, 0)
        val upEvent2 = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 200f, 200f, 0)
        
        listener.onTouch(mockBubbleView, downEvent2)
        listener.onTouch(mockBubbleView, moveEvent)
        listener.onTouch(mockBubbleView, upEvent2)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        
        assertTrue("Drag should NOT open notepad", !service.testNotepadOpened)
        println("Drag: Verified (No Open)")
    }
}
