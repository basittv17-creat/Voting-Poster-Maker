package com.awisogera.votingpostermaker

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import android.graphics.Bitmap
import kotlin.math.roundToInt
import org.junit.Test
import java.util.UUID

class EditorNavigationTest {
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @Test
    fun eachModeOpensEditorAndBackClosesExportFirst() {
        ActivityScenario.launch(MainActivity::class.java).use {
            for ((mode, template) in listOf("Voting Poster" to "Blue vs Purple", "Single Poster Design" to "Midnight Portrait")) {
                awaitText("Create a poster")
                tap(mode)
                tap(template)
                tap("Export")
                awaitText("PNG")
                back()
                awaitCondition("Export panel did not close") { nodes().none { it.text?.toString() == "PNG" } }
                tap("Home")
                awaitText("Create a poster")
            }
        }
    }

    @Test
    fun touchingAndDraggingPhotosChangesOnlyTheIntendedLayer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val store = ProjectStore(context)
        val original = File.createTempFile("ui-portrait", ".jpg", context.cacheDir)
        val imported = mutableListOf<String>()
        val draft = templates(PosterMode.VOTING).first().create(PosterMode.VOTING).copy(title = "A/B touch fixture")
        try {
            instrumentation.context.assets.open("astronaut.jpg").use { input ->
                original.outputStream().use { input.copyTo(it) }
            }
            val seeded = runBlocking {
                val layers = draft.layers.map { layer ->
                    val key = store.import(Uri.fromFile(original))
                    imported += key
                    layer.copy(asset = key)
                }
                draft.copy(layers = layers).also { store.save(it) }
            }
            ActivityScenario.launch(MainActivity::class.java).use {
                awaitText("Create a poster")
                tap("My Designs")
                tap(seeded.title)
                var expected = seeded.layers
                for (layer in seeded.layers) {
                    val (x, y) = centerOnCanvas(layer)
                    shell("input tap $x $y")
                    awaitText(layer.name)
                    val selected = persisted(store, seeded.id) { it.selectedId == layer.id }
                    assertEquals(expected, selected.layers)

                    val (startX, startY) = centerOnCanvas(layer)
                    shell("input swipe $startX $startY ${startX + 32} $startY 400")
                    val moved = persisted(store, seeded.id) { it.selectedId == layer.id && it.selected!!.x > layer.x }
                    assertEquals(expected.filterNot { it.id == layer.id }, moved.layers.filterNot { it.id == layer.id })
                    tap("Undo")
                    persisted(store, seeded.id) { it.layers == expected }
                    tap("Redo")
                    persisted(store, seeded.id) { it.layers == moved.layers }
                    expected = moved.layers
                }
            }
        } finally {
            runBlocking { PosterDatabase.get(context).posters().delete(draft.id) }
            imported.forEach { store.asset(it).delete() }
            original.delete()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    @androidx.annotation.RequiresApi(33)
    fun systemPhotoPickerImportsAndReplacesEachVotingAndSingleSlot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val store = ProjectStore(context)
        val imported = mutableListOf<String>()
        val drafts = PosterMode.entries.map { mode ->
            templates(mode).first().create(mode).copy(title = "${mode.title} picker fixture")
        }
        try {
            runBlocking { drafts.forEach { store.save(it) } }
            ActivityScenario.launch(MainActivity::class.java).use {
                for (draft in drafts) {
                    awaitText("Create a poster")
                    tap("My Designs")
                    tap(draft.title)
                    var expected = draft.layers
                    for (slot in draft.layers.filter { it.slot != null }) {
                        for ((index, color) in listOf(Color.RED, Color.BLUE).withIndex()) {
                            val fixture = requireNotNull(resolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, "vpm-picker-${UUID.randomUUID()}.png")
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VpmPickerTest")
                                    put(MediaStore.Images.Media.IS_PENDING, 1)
                                },
                            ))
                            try {
                                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                                try {
                                    bitmap.eraseColor(color)
                                    requireNotNull(resolver.openOutputStream(fixture)).use {
                                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                                    }
                                } finally { bitmap.recycle() }
                                assertEquals(1, resolver.update(fixture, ContentValues().apply {
                                    put(MediaStore.Images.Media.IS_PENDING, 0)
                                }, null, null))

                                tap(requireNotNull(slot.slot))
                                if (index > 0) tap("Replace photo")
                                val thumbnail = awaitNode("System Photo Picker photo") { node ->
                                    node.packageName?.toString()?.let { pkg ->
                                        pkg.contains("providers.media") || pkg.contains("photopicker")
                                    } == true && node.isClickable && node.isLongClickable &&
                                        !node.contentDescription.isNullOrBlank()
                                }
                                touch(thumbnail, "system Photo Picker thumbnail")
                                val previous = expected.first { it.id == slot.id }.asset
                                val result = persisted(store, draft.id) {
                                    val asset = it.layers.first { layer -> layer.id == slot.id }.asset
                                    asset != null && asset != previous
                                }
                                val asset = requireNotNull(result.layers.first { it.id == slot.id }.asset)
                                imported += asset
                                val selected = requireNotNull(BitmapFactory.decodeFile(store.asset(asset).path))
                                try {
                                    assertEquals(color, selected.getPixel(selected.width / 2, selected.height / 2))
                                } finally { selected.recycle() }
                                assertEquals(expected.filterNot { it.id == slot.id }, result.layers.filterNot { it.id == slot.id })
                                expected = result.layers
                            } finally {
                                resolver.delete(fixture, null, null)
                            }
                        }
                    }
                    tap("Home")
                }
            }
        } finally {
            runBlocking { drafts.forEach { PosterDatabase.get(context).posters().delete(it.id) } }
            imported.forEach { store.asset(it).delete() }
        }
    }

    private fun centerOnCanvas(layer: PosterLayer): Pair<Int, Int> {
        val bounds = Rect()
        awaitNode("Poster canvas") { it.contentDescription?.toString() == "Poster canvas" }.getBoundsInScreen(bounds)
        val scale = minOf(bounds.width() / 1200f, bounds.height() / 900f)
        val x = bounds.exactCenterX() + (layer.x - 600f) * scale
        val y = bounds.exactCenterY() + (layer.y - 450f) * scale
        return x.roundToInt() to y.roundToInt()
    }

    private fun persisted(store: ProjectStore, id: String, matches: (PosterDocument) -> Boolean): PosterDocument =
        runBlocking {
            withTimeout(8_000) {
                store.projects.first { records ->
                    records.any { it.id == id && matches(store.read(it)) }
                }.first { it.id == id }.let(store::read)
            }
        }

    private fun nodes(): List<AccessibilityNodeInfo> = buildList {
        fun visit(node: AccessibilityNodeInfo) {
            add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        automation.rootInActiveWindow?.let(::visit)
    }

    private fun awaitText(label: String): AccessibilityNodeInfo = awaitNode(label) { hasLabel(it, label) }

    private fun hasLabel(node: AccessibilityNodeInfo, label: String): Boolean {
        val text = node.text?.toString().orEmpty()
        return text == label || text.startsWith("$label,") || text.startsWith("$label\n")
    }

    private fun awaitNode(label: String, matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 8_000
        do {
            nodes().firstOrNull { it.isVisibleToUser && matches(it) }?.let { return it }
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        automation.takeScreenshot()?.let { screenshot ->
            try {
                File(context.cacheDir, "navigation-failure.png").outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                screenshot.recycle()
            }
        }
        val visible = nodes().filter { it.isVisibleToUser }
            .map { "${it.packageName}: ${(it.text ?: it.contentDescription)?.toString().orEmpty()}" }
            .distinct().take(25)
        error("Text not visible: $label. Visible nodes: $visible")
    }

    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 8_000
        do {
            if (condition()) return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        error(message)
    }

    private fun tap(label: String) {
        val findText = { nodes().firstOrNull { it.isVisibleToUser && hasLabel(it, label) } }
        var candidate = findText()
        repeat(3) {
            if (candidate == null) {
                val scroll = nodes().firstOrNull { it.isVisibleToUser && it.isScrollable }
                if (scroll?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true) {
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    candidate = findText()
                }
            }
        }
        touch(candidate ?: awaitText(label), label)
    }

    private fun touch(node: AccessibilityNodeInfo, label: String) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        assertTrue("No visible touch bounds for $label", !bounds.isEmpty)
        shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
    }

    private fun back() = shell("input keyevent KEYCODE_BACK")

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
            it.readBytes()
        }
    }
}
