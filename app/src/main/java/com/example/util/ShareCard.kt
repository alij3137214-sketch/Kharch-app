package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseCategory
import com.example.domain.MoneyMath
import com.example.domain.PeriodSummary
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Draws a "my spending story" picture that can be sent to anyone.
 * It is drawn by hand on a bitmap so it looks the same on every phone.
 */
object ShareCard {

    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val BG = 0xFF0A0B0D.toInt()
    private const val SURFACE = 0xFF16171B.toInt()
    private const val INK = 0xFFF5F5F6.toInt()
    private const val MUTED = 0xFF9A9CA4.toInt()
    private const val ACCENT = 0xFF4FD6A0.toInt()
    private const val RED = 0xFFFF7468.toInt()
    private const val MARGIN = 72f

    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = align
        }

    private fun money(v: Double) = "Rs. " + String.format(Locale.US, "%,.0f", v)

    fun render(
        summary: PeriodSummary,
        previousSpent: Double,
        showAmounts: Boolean,
        name: String
    ): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(BG)
        val right = WIDTH - MARGIN

        // Top row: brand and period
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        c.drawCircle(MARGIN + 16f, 120f, 16f, dot)
        c.drawText("Kharch", MARGIN + 52f, 134f, paint(INK, 46f, bold = true))
        c.drawText(summary.range.label, right, 134f, paint(MUTED, 42f, align = Paint.Align.RIGHT))

        // Hero
        val owner = if (name.isBlank()) "My spending" else "$name's spending"
        c.drawText(owner, MARGIN, 290f, paint(MUTED, 46f))
        if (showAmounts) {
            c.drawText(money(summary.spent), MARGIN, 440f, paint(INK, 150f, bold = true))
        } else {
            c.drawText(summary.range.label, MARGIN, 440f, paint(INK, 120f, bold = true))
        }

        val change = MoneyMath.changeText(summary.spent, previousSpent)
        if (change != null) {
            val lessIsGood = summary.spent <= previousSpent
            val chipColor = if (lessIsGood) ACCENT else RED
            val chipPaint = paint(chipColor, 42f, bold = true)
            val w = chipPaint.measureText(change) + 64f
            val chip = RectF(MARGIN, 490f, MARGIN + w, 574f)
            c.drawRoundRect(chip, 42f, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (chipColor and 0x00FFFFFF) or 0x28000000 })
            c.drawText(change, MARGIN + 32f, 548f, chipPaint)
        }

        // Where it went
        c.drawText("Where it went", MARGIN, 680f, paint(MUTED, 42f))
        val top = summary.byCategory.take(4)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SURFACE }
        val barWidth = WIDTH - MARGIN * 2
        top.forEachIndexed { i, cat ->
            val y = 760f + i * 118f
            val color = ExpenseCategory.fromString(cat.category).color.let {
                android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt())
            }
            val pct = "${(cat.share * 100).toInt()}%"
            val value = if (showAmounts) "${money(cat.amount)}  ·  $pct" else pct
            val valuePaint = paint(INK, 42f, bold = true, align = Paint.Align.RIGHT)
            val room = barWidth - valuePaint.measureText(value) - 24f
            val label = TextUtils.ellipsize(cat.category, paint(INK, 44f), room, TextUtils.TruncateAt.END).toString()
            c.drawText(label, MARGIN, y, paint(INK, 44f))
            c.drawText(value, right, y, valuePaint)
            c.drawRoundRect(RectF(MARGIN, y + 20f, right, y + 44f), 12f, 12f, trackPaint)
            val fill = (barWidth * cat.share).coerceAtLeast(24f)
            c.drawRoundRect(
                RectF(MARGIN, y + 20f, MARGIN + fill, y + 44f), 12f, 12f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            )
        }
        if (top.isEmpty()) {
            c.drawText("No spending yet", MARGIN, 770f, paint(MUTED, 44f))
        }

        // Small facts
        val facts = buildList {
            if (summary.noSpendDays > 0) add("${summary.noSpendDays} ${if (summary.noSpendDays == 1) "day" else "days"} with no spending")
            if (summary.transactionCount > 0) add("${summary.transactionCount} records")
        }.joinToString("   ·   ")
        if (facts.isNotEmpty()) c.drawText(facts, MARGIN, 1250f, paint(ACCENT, 40f, bold = true))

        c.drawText("Made with Kharch", WIDTH / 2f, 1310f, paint(MUTED, 34f, align = Paint.Align.CENTER))
        return bmp
    }

    /** Saves the picture in the cache folder that the app is allowed to share from. */
    fun save(context: Context, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "kharch_story_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun shareIntent(context: Context, file: File, text: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
