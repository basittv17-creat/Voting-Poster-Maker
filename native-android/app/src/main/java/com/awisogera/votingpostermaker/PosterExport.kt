package com.awisogera.votingpostermaker

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

enum class ExportFormat(val extension: String, val mime: String, val format: Bitmap.CompressFormat) {
    PNG("png", "image/png", Bitmap.CompressFormat.PNG),
    JPEG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG)
}

class PosterExport(private val context: Context, private val renderer: PosterRenderer) {
    suspend fun render(document: PosterDocument, edge: Int, format: ExportFormat): File = withContext(Dispatchers.IO) {
        val bitmaps = mutableMapOf<String, Bitmap>()
        val folder = File(context.cacheDir, "exports").apply { mkdirs() }
        val output = File(folder, "Voting-Poster-Maker-${UUID.randomUUID()}.${format.extension}")
        try {
            val ratio = edge.toFloat() / maxOf(document.width, document.height)
            val width = (document.width * ratio).roundToInt().coerceAtLeast(1)
            val height = (document.height * ratio).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val keys = renderer.keys(document)
                val decodeLimit = minOf(edge, kotlin.math.sqrt(12_000_000.0 / keys.size.coerceAtLeast(1)).toInt())
                keys.forEach { key -> bitmaps[key] = requireNotNull(renderer.load(key, decodeLimit)) { "A poster asset is missing." } }
                val canvas = Canvas(bitmap)
                if (format == ExportFormat.JPEG) canvas.drawColor(Color.WHITE)
                canvas.scale(width / 1200f, height / 900f)
                renderer.draw(canvas, document, bitmaps, editing = false)
                output.outputStream().use { check(bitmap.compress(format.format, 94, it)) }
            } finally {
                bitmap.recycle()
            }
            output
        } catch (failure: Throwable) {
            output.delete()
            throw failure
        } finally {
            bitmaps.values.forEach(Bitmap::recycle)
        }
    }

    fun shareUri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun copyTo(file: File, uri: Uri) = withContext(Dispatchers.IO) {
        requireNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
            file.inputStream().use { it.copyTo(output) }
        }
    }

    suspend fun saveToGallery(file: File, format: ExportFormat): Uri = withContext(Dispatchers.IO) {
        check(Build.VERSION.SDK_INT >= 29)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Voting Poster Maker")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        try {
            copyTo(file, uri)
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) == 1)
            uri
        } catch (failure: Throwable) {
            resolver.delete(uri, null, null)
            throw failure
        }
    }
}
