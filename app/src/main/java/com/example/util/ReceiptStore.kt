package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Photos picked from the gallery can stop working after a restart. This copies the photo
 * into the app's own storage, so it is still there later.
 */
object ReceiptStore {

    /** Returns a file:// address of the copy, or null if copying failed. */
    fun save(context: Context, source: Uri): String? = runCatching {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val target = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        Uri.fromFile(target).toString()
    }.getOrNull()

    /** Deletes a copy made by [save]. Other addresses are left alone. */
    fun delete(address: String?) {
        if (address.isNullOrBlank() || !address.startsWith("file://")) return
        runCatching { File(Uri.parse(address).path ?: return).delete() }
    }
}
