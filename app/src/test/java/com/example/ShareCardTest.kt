package com.example

import android.graphics.Bitmap
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.MoneyMath
import com.example.domain.PeriodKind
import com.example.util.ShareCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

/** Draws the picture people can share, and saves it so it can be looked at. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ShareCardTest {

    private fun at(d: Int) = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, d, 12, 0) }.timeInMillis

    private fun sample(): List<TransactionEntity> = listOf(
        TransactionEntity(title = "Groceries", amount = 18500.0, type = TransactionType.EXPENSE.name, category = "Food", paymentMethod = "Cash", timestamp = at(3)),
        TransactionEntity(title = "Fuel", amount = 9200.0, type = TransactionType.EXPENSE.name, category = "Transport", paymentMethod = "Cash", timestamp = at(5)),
        TransactionEntity(title = "Shoes", amount = 7800.0, type = TransactionType.EXPENSE.name, category = "Shopping", paymentMethod = "Card", timestamp = at(8)),
        TransactionEntity(title = "Doctor", amount = 3500.0, type = TransactionType.EXPENSE.name, category = "Health", paymentMethod = "Cash", timestamp = at(9)),
        TransactionEntity(title = "Movie", amount = 1500.0, type = TransactionType.EXPENSE.name, category = "Entertainment", paymentMethod = "Cash", timestamp = at(12)),
        TransactionEntity(title = "Salary", amount = 150000.0, type = TransactionType.INCOME.name, category = "Salary", paymentMethod = "Bank", timestamp = at(1))
    )

    @Test
    fun picture_isTheRightSize_andCanBeSaved() {
        val now = at(15)
        val summary = MoneyMath.summarize(sample(), MoneyMath.periodRange(PeriodKind.MONTH, 0, now), now)
        val withAmounts = ShareCard.render(summary, previousSpent = 52000.0, showAmounts = true, name = "Ali")
        val percentOnly = ShareCard.render(summary, previousSpent = 52000.0, showAmounts = false, name = "")
        assertEquals(ShareCard.WIDTH, withAmounts.width)
        assertEquals(ShareCard.HEIGHT, withAmounts.height)
        assertEquals(ShareCard.WIDTH, percentOnly.width)

        val dir = File("build/preview").apply { mkdirs() }
        File(dir, "share_card_amounts.png").outputStream().use { withAmounts.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File(dir, "share_card_percent.png").outputStream().use { percentOnly.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(File(dir, "share_card_amounts.png").length() > 10_000)
    }

    @Test
    fun picture_worksWithNoSpendingAtAll() {
        val now = at(15)
        val summary = MoneyMath.summarize(emptyList(), MoneyMath.periodRange(PeriodKind.MONTH, 0, now), now)
        val bmp = ShareCard.render(summary, previousSpent = 0.0, showAmounts = true, name = "")
        assertEquals(ShareCard.HEIGHT, bmp.height)
    }
}