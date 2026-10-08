package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CsvExporter {

    /**
     * Converts a list of expense transactions into RFC 4180 compliant CSV format.
     */
    fun generateMonthlyExpensesCsv(
        expenses: List<TransactionEntity>,
        monthYearLabel: String
    ): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val totalAmount = expenses.sumOf { it.amount }

        val sb = StringBuilder()
        // Header
        sb.append("Date,Time,Title,Category,Amount,Payment Method,Note\n")

        for (tx in expenses) {
            val dateStr = dateFmt.format(Date(tx.timestamp))
            val timeStr = timeFmt.format(Date(tx.timestamp))
            val titleEscaped = escapeCsv(tx.title)
            val categoryEscaped = escapeCsv(tx.category)
            val amountStr = String.format(Locale.US, "%.2f", tx.amount)
            val paymentMethodEscaped = escapeCsv(tx.paymentMethod)
            val noteEscaped = escapeCsv(tx.note)

            sb.append("\"$dateStr\",\"$timeStr\",$titleEscaped,$categoryEscaped,$amountStr,$paymentMethodEscaped,$noteEscaped\n")
        }

        return sb.toString()
    }

    /**
     * Escapes fields according to standard CSV guidelines (RFC 4180).
     */
    fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Generates a standard filename for the monthly expense CSV export.
     */
    fun getMonthlyCsvFilename(cal: Calendar): String {
        val fileMonth = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(cal.time)
        return "kharch_expenses_$fileMonth.csv"
    }

    /**
     * Writes CSV string to cache directory so it can be securely shared via FileProvider.
     */
    fun exportCsvToCacheFile(context: Context, filename: String, csvContent: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, filename)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(csvContent)
                writer.flush()
            }
        }
        return file
    }

    /**
     * Creates an ACTION_SEND Intent with FileProvider URI to share with external tools.
     */
    fun createShareCsvIntent(context: Context, file: File, subject: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Writes CSV text to a user-selected document Uri (e.g. from Storage Access Framework).
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copies CSV text to the system clipboard.
     */
    fun copyCsvToClipboard(context: Context, csvContent: String, label: String = "Monthly Expense CSV") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, csvContent)
        clipboard.setPrimaryClip(clip)
    }
}
