package com.awisogera.votingpostermaker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfflinePosterTest {
    @Test
    fun localImportCutoutBackgroundAndExportInAirplaneMode() {
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            assertEquals("Enable airplane mode before this scenario", 1,
                Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0))

            val original = File(context.cacheDir, "offline-test-portrait.jpg")
            instrumentation.context.assets.open("astronaut.jpg").use { input ->
                original.outputStream().use { input.copyTo(it) }
            }
            val store = ProjectStore(context)
            val renderer = PosterRenderer(context, store)
            val segmenter = BackgroundCutout(store, renderer)
            val exporter = PosterExport(context, renderer)

            for (mode in PosterMode.entries) {
                var document = templates(mode).first().create(mode)
                val originalKeys = mutableListOf<String>()
                try {
                    for (slot in document.layers.filter { it.slot != null }) {
                        val imported = store.import(Uri.fromFile(original))
                        originalKeys += imported
                        document = document.updateLayer(slot.id) { it.copy(asset = imported) }
                    }
                    for (slot in document.layers.filter { it.slot != null }) {
                        val others = document.layers.filter { it.id != slot.id }
                        val cutout = segmenter.create(requireNotNull(slot.asset))
                        originalKeys += cutout
                        val bitmap = requireNotNull(renderer.load(cutout, 360))
                        assertMask(bitmap)
                        val replacement = Backdrop(if (slot.slot == "B") 0xff551511 else 0xff184b86)
                        document = document.updateLayer(slot.id) { it.copy(cutout = cutout, backdrop = replacement) }
                        assertEquals(others, document.layers.filter { it.id != slot.id })
                        assertTrue(store.asset(requireNotNull(slot.asset)).isFile)
                    }
                    store.save(document)
                    val saved = requireNotNull(store.projects.first().find { it.id == document.id })
                    val reopened = store.read(saved)
                    assertEquals(document, reopened)
                    val file = exporter.render(reopened, 1280, ExportFormat.PNG)
                    try {
                        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path))
                        try {
                            assertEquals(1280, bitmap.width)
                            assertEquals(960, bitmap.height)
                        } finally { bitmap.recycle() }
                    } finally { file.delete() }
                } finally {
                    PosterDatabase.get(context).posters().delete(document.id)
                    originalKeys.forEach { store.asset(it).delete() }
                }
            }
            original.delete()
        }
    }

    private fun assertMask(bitmap: Bitmap) {
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue("The model removed visible background", pixels.count { it ushr 24 < 20 } > pixels.size / 20)
            assertTrue("The model retained visible foreground", pixels.count { it ushr 24 > 230 } > pixels.size / 20)
        } finally {
            bitmap.recycle()
        }
    }
}
