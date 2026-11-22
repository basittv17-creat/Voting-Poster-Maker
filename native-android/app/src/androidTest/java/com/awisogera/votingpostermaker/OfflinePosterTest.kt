package com.awisogera.votingpostermaker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
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
                    for (format in ExportFormat.entries) {
                        val file = exporter.render(reopened, 1280, format)
                        try {
                            val share = exporter.shareUri(file)
                            assertEquals(format.mime, context.contentResolver.getType(share))
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            requireNotNull(context.contentResolver.openInputStream(share)).use {
                                BitmapFactory.decodeStream(it, null, bounds)
                            }
                            assertEquals(1280, bounds.outWidth)
                            assertEquals(960, bounds.outHeight)
                            assertEquals(format.mime, bounds.outMimeType)
                            if (Build.VERSION.SDK_INT >= 29) {
                                val gallery = exporter.saveToGallery(file, format)
                                try {
                                    assertEquals(format.mime, context.contentResolver.getType(gallery))
                                    requireNotNull(context.contentResolver.openInputStream(gallery)).use {
                                        assertTrue(it.read() >= 0)
                                    }
                                } finally {
                                    context.contentResolver.delete(gallery, null, null)
                                }
                            }
                        } finally {
                            file.delete()
                        }
                    }
                } finally {
                    PosterDatabase.get(context).posters().delete(document.id)
                    originalKeys.forEach { store.asset(it).delete() }
                }
            }
            original.delete()
        }
    }

    @Test
    fun exportsMultilineUrduAndMixedText() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = ProjectStore(context)
            val export = PosterExport(context, PosterRenderer(context, store))
            val text = PosterLayer(
                kind = LayerKind.TEXT, name = "Urdu headline", width = 1080f, height = 350f,
                text = "اپنا ووٹ ضرور ڈالیں\nپاکستان · Vote 2026", fontSize = 85f,
            )
            val document = PosterDocument(PosterMode.SINGLE, background = Backdrop(), layers = listOf(text))
            val rendered = export.render(document, 1200, ExportFormat.PNG)
            try {
                val bitmap = requireNotNull(BitmapFactory.decodeFile(rendered.path))
                try {
                    val row = IntArray(bitmap.width)
                    var inkRows = 0
                    for (y in 0 until bitmap.height) {
                        bitmap.getPixels(row, 0, row.size, 0, y, row.size, 1)
                        if (row.count { it ushr 24 > 100 } > 10) inkRows++
                    }
                    assertTrue("Text has substantial ink in the export", inkRows > 60)
                    assertEquals(0, bitmap.getPixel(0, 0) ushr 24)
                } finally {
                    bitmap.recycle()
                }
                rendered.copyTo(File(context.cacheDir, "urdu-export.png"), overwrite = true)
            } finally {
                rendered.delete()
            }
        }
    }

    @Test
    fun brushErasesAndRestoresOriginalPixelsWithFeathering() {
        val original = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        original.eraseColor(android.graphics.Color.RED)
        val edited = requireNotNull(original.copy(Bitmap.Config.ARGB_8888, true))
        try {
            val brush = MaskBrush(original, edited)
            brush.dab(32.5f, 32.5f, 20f, .5f, restore = false)
            assertEquals(0, edited.getPixel(32, 32) ushr 24)
            assertTrue(edited.getPixel(47, 32) ushr 24 in 1..254)
            assertEquals(android.graphics.Color.RED, edited.getPixel(0, 0))
            brush.dab(32.5f, 32.5f, 10f, .5f, restore = true)
            assertEquals(android.graphics.Color.RED, edited.getPixel(32, 32))
            assertEquals(android.graphics.Color.RED, original.getPixel(32, 32))
            brush.stroke(53.5f, 10.5f, 10.5f, 10.5f, 4f, 0f, restore = false)
            assertEquals(0, edited.getPixel(30, 10) ushr 24)
        } finally {
            original.recycle()
            edited.recycle()
        }
    }

    @Test
    fun exportsLayeredPortraitAt3840Pixels() {
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val store = ProjectStore(context)
            val source = store.newAsset()
            try {
                instrumentation.context.assets.open("astronaut.jpg").use { input ->
                    source.outputStream().use { input.copyTo(it) }
                }
                val document = templates(PosterMode.VOTING).first().create(PosterMode.VOTING)
                val layered = document.copy(layers = document.layers.map { it.copy(asset = source.name) })
                val exporter = PosterExport(context, PosterRenderer(context, store))
                val file = exporter.render(layered, 3840, ExportFormat.PNG)
                try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    assertEquals(3840, bounds.outWidth)
                    assertEquals(2880, bounds.outHeight)
                    assertEquals("image/png", bounds.outMimeType)
                } finally {
                    file.delete()
                }
            } finally {
                source.delete()
            }
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
