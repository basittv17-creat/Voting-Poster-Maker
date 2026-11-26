package com.awisogera.votingpostermaker

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BackgroundCutout(private val store: ProjectStore, private val renderer: PosterRenderer) {
    suspend fun create(key: String): String = withContext(Dispatchers.Default) {
        val source = requireNotNull(renderer.load(key, 1536)) { "This photo could not be decoded." }
        val client = Segmentation.getClient(SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).build())
        val target = store.newAsset()
        try {
            // The SDK continues processing its input until the task completes.
            val mask = withContext(NonCancellable) {
                client.process(InputImage.fromBitmap(source, 0)).await()
            }
            currentCoroutineContext().ensureActive()
            require(mask.width == source.width && mask.height == source.height) { "Unexpected mask size." }
            val confidence = mask.buffer.asFloatBuffer()
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            try {
                val row = IntArray(source.width)
                var foreground = false
                for (y in 0 until source.height) {
                    currentCoroutineContext().ensureActive()
                    source.getPixels(row, 0, row.size, 0, y, row.size, 1)
                    for (x in row.indices) {
                        val value = confidence.get()
                        val feather = ((value - .2f) / .65f).coerceIn(0f, 1f)
                        val alpha = (feather * (row[x] ushr 24)).toInt()
                        if (alpha > 180) foreground = true
                        row[x] = (row[x] and 0xffffff) or (alpha shl 24)
                    }
                    output.setPixels(row, 0, row.size, 0, y, row.size, 1)
                }
                require(foreground) { "No clear person was detected in this photo." }
                withContext(Dispatchers.IO) {
                    target.outputStream().use { check(output.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                }
            } finally {
                output.recycle()
            }
            target.name
        } catch (failure: Throwable) {
            target.delete()
            throw failure
        } finally {
            client.close()
            source.recycle()
        }
    }
}
