package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.KharchDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtEntity
import com.example.data.model.WishItemEntity
import com.example.data.profile.Goal
import com.example.data.profile.IncomeType
import com.example.data.profile.ProfileStore
import com.example.data.profile.UserProfile
import com.example.data.repository.KharchRepository
import com.example.domain.MoneyMath
import com.example.ui.theme.KharchTheme
import com.example.ui.viewmodel.KharchViewModel
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Boots the real app on an in-memory database and takes pictures of every screen. Output: build/preview/app_*.png */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [36])
class AppPreviewTest {

    @get:Rule val rule = createComposeRule()

    private fun launch(
        seed: Boolean = true,
        onboarded: Boolean = true,
        goal: Goal = Goal.TRACK,
        incomeType: IncomeType = IncomeType.MONTHLY
    ) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(ctx, KharchDatabase::class.java).allowMainThreadQueries().build()
        val repo = KharchRepository(db.kharchDao())
        if (seed) runBlocking {
            repo.seedRandomTestingData()
            val now = System.currentTimeMillis()
            repo.insertBudget(BudgetEntity(category = "OVERALL", monthlyLimit = 135000.0, monthYear = MoneyMath.monthKey(now)))
            repo.addDebt(DebtEntity(person = "Ali", amount = 5000.0, direction = "LENT", paidAmount = 1000.0, note = "For the bike repair"))
            repo.addDebt(DebtEntity(person = "Sara", amount = 2000.0, direction = "BORROWED"))
            repo.addWishItem(WishItemEntity(title = "Headphones", price = 8000.0, addedAt = now - 4 * 86_400_000L, waitDays = 3))
            repo.addWishItem(WishItemEntity(title = "Leather jacket", price = 14000.0, addedAt = now - 86_400_000L, waitDays = 7))
            repo.addCommittee(CommitteeEntity(name = "Office committee", monthlyAmount = 5000.0, totalMembers = 10, myTurn = 3, startMonth = MoneyMath.monthKey(now), paidMonths = 0))
        }
        val store = ProfileStore.memory().also {
            it.save(
                UserProfile(
                    onboardingDone = onboarded, name = if (onboarded) "Ali" else "", goal = goal, incomeType = incomeType,
                    monthlyIncome = if (onboarded) 150000.0 else 0.0, savePercent = 10, hoursPerDay = 8, daysPerMonth = 26
                )
            )
        }
        val vm = KharchViewModel(repo, store)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            KharchTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    KharchMainApp(viewModel = vm)
                }
            }
        }
        advance(6, 250)
    }

    private fun advance(times: Int = 4, sleepMs: Long = 120) {
        repeat(times) {
            Thread.sleep(sleepMs)
            rule.mainClock.advanceTimeBy(500)
        }
    }

    private fun shot(name: String) {
        advance(5, 120)
        captureScreenRoboImage("build/preview/app_$name.png")
    }

    private fun tab(tag: String) {
        rule.onNodeWithTag(tag).performClick()
        rule.mainClock.advanceTimeBy(600)
    }

    private fun next() {
        rule.onNodeWithTag("onboarding_next").performClick()
        advance(2, 60)
    }

    // ---- Main tabs -----------------------------------------------------------------------

    @Test fun home() { launch(); tab("nav_activity"); tab("nav_home"); shot("home") }

    @Test fun home_goalSave() { launch(goal = Goal.SAVE); tab("nav_activity"); tab("nav_home"); shot("home_save") }

    @Test fun home_goalBills_dailyIncome() { launch(goal = Goal.BILLS, incomeType = IncomeType.DAILY); tab("nav_activity"); tab("nav_home"); shot("home_bills") }

    @Test fun home_newUserNoData() { launch(seed = false); shot("home_empty") }

    @Test fun history() { launch(); tab("nav_activity"); shot("history") }

    @Test fun charts() { launch(); tab("nav_understand"); shot("charts") }

    @Test fun plan() { launch(); tab("nav_plan"); shot("plan") }

    @Test fun add() {
        launch(); tab("nav_add")
        rule.onNodeWithTag("amount_input").performTextInput("2500")
        shot("add")
    }

    // ---- First-run questions -------------------------------------------------------------

    @Test fun onboarding_steps() {
        launch(seed = false, onboarded = false)
        shot("onb_1_name")
        next(); shot("onb_2_goal")
        next(); shot("onb_3_incometype")
        next()
        rule.onNode(hasSetTextAction()).performTextInput("150000")
        shot("onb_4_income")
        next(); shot("onb_5_save")
        next(); shot("onb_6_bills")
        next(); shot("onb_7_categories")
        next(); shot("onb_8_work")
        next(); shot("onb_9_reminders")
        next(); shot("onb_10_plan")
    }

    // ---- Helpers and sheets --------------------------------------------------------------

    /** Scrolling needs the animation clock to run by itself, so it is switched on just for the scroll. */
    private fun scrollPlanTo(title: String) {
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("plan_screen").performScrollToNode(hasText(title))
        rule.mainClock.autoAdvance = false
    }

    private fun openHelper(title: String, name: String) {
        launch(); tab("nav_plan")
        scrollPlanTo(title)
        rule.onNodeWithText(title).performClick()
        rule.mainClock.advanceTimeBy(600)
        shot(name)
    }

    @Test fun helper_canBuy() {
        launch(); tab("nav_plan")
        scrollPlanTo("Can I buy it?")
        rule.onNodeWithText("Can I buy it?").performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.onNode(hasSetTextAction() and hasText("Price")).performTextInput("45000")
        shot("helper_canbuy")
    }

    @Test fun helper_waitList() = openHelper("Wait list", "helper_wait")

    @Test fun helper_udhaar() = openHelper("Udhaar", "helper_udhaar")

    @Test fun helper_committee() = openHelper("Committee", "helper_committee")

    @Test fun sheet_deleteData() { launch(); rule.onNodeWithTag("home_delete_data_button").performClick(); shot("sheet_delete") }

    @Test fun sheet_settings() { launch(); rule.onNodeWithTag("home_settings_button").performClick(); shot("sheet_settings") }

    @Test fun sheet_sharePicture() { launch(); tab("nav_understand"); rule.onNodeWithTag("analytics_share_visual_btn").performClick(); shot("sheet_share") }
}
