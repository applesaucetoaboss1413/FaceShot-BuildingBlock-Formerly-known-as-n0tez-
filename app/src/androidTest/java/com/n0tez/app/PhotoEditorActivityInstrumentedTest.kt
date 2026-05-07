package com.n0tez.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoEditorActivityInstrumentedTest {
    @Test
    fun launchesAndShowsToolButtons() {
        ActivityScenario.launch(PhotoEditorActivity::class.java).use {
            onView(withId(R.id.btnAdjust)).check(matches(isDisplayed()))
            onView(withId(R.id.btnFilters)).check(matches(isDisplayed()))
            onView(withId(R.id.btnDraw)).check(matches(isDisplayed()))
        }
    }
}

