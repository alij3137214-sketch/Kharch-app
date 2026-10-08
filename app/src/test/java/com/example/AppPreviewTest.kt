package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.KharchDatabase
import com.example.data.repository.KharchRepository
import com.example.ui.theme.KharchTheme
import com.example.ui.viewmodel.KharchViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Boots the real app on an in-memory database and captures every tab. Output: build/preview/app_*.png */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [36])
class AppPreviewTest {

    @get:Rule val rule = createComposeRule()

    private fun launch(seed: Boolean = true) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(ctx, KharchDatabase::class.java).allowMainThreadQueries().build()
        val repo = KharchRepository(db.kharchDao(), db.expenseDao())
        if (seed) runBlocking { repo.seedRandomTestingData() }
        val vm = KharchViewModel(repo)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            KharchTheme {
                Box(Modifier.size(411.dp, 891.dp).background(MaterialTheme.colorScheme.background)) {
                    KharchMainApp(viewModel = vm)
                }
            }
        }
        repeat(6) {
            Thread.sleep(250)
            rule.mainClock.advanceTimeBy(500)
        }
    }

    private fun shot(name: String) {
        repeat(5) {
            Thread.sleep(150)
            rule.mainClock.advanceTimeBy(500)
        }
        rule.onRoot().captureRoboImage("build/preview/app_$name.png")
    }

    private fun tab(tag: String) {
        rule.onNodeWithTag(tag).performClick()
        rule.mainClock.advanceTimeBy(600)
    }

    @Test fun home() { launch(); tab("nav_activity"); tab("nav_home"); shot("home") }

    @Test fun activity() { launch(); tab("nav_activity"); shot("activity") }

    @Test fun insights() { launch(); tab("nav_understand"); shot("insights") }

    @Test fun plan() { launch(); tab("nav_plan"); shot("plan") }

    @Test fun add() { launch(); tab("nav_add"); shot("add") }
}
