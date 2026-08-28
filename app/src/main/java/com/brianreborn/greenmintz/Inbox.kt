package com.brianreborn.greenmintz

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.File

internal object Inbox {
    fun streamUris(intent: Intent): List<Uri> {
        val out = mutableListOf<Uri>()
        extraStream(intent)?.let { out += it }
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            extraStreamList(intent).forEach { out += it }
        }
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.let { out += it }
            }
        }
        return out.distinct()
    }

    fun copy(context: Context, uri: Uri): Pair<String, Uri>? {
        val dir = File(context.filesDir, "inbox").apply { mkdirs() }
        val raw = displayName(context, uri) ?: "shared.bin"
        val safe = raw.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "shared.bin" }
        val dest = File(dir, "${System.currentTimeMillis()}_$safe")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
        }.getOrNull() ?: return null
        if (!dest.isFile || dest.length() == 0L) {
            dest.delete()
            return null
        }
        return safe to Uri.fromFile(dest)
    }

    private fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }

    private fun extraStream(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    private fun extraStreamList(intent: Intent): List<Uri> =
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }
}
