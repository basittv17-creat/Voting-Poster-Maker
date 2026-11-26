package com.awisogera.votingpostermaker

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.view.InputDevice
import android.view.MotionEvent
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
                awaitText(template)
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
                    tapDock("Undo")
                    persisted(store, seeded.id) { it.layers == expected }
                    tapDock("Redo")
                    persisted(store, seeded.id) { it.layers == moved.layers }
                    val movedLayer = moved.layers.first { it.id == layer.id }
                    val (pinchX, pinchY) = centerOnCanvas(movedLayer)
                    pinchAndRotate(pinchX, pinchY)
                    val transformed = persisted(store, seeded.id) {
                        val actual = it.layers.first { candidate -> candidate.id == layer.id }
                        actual.scale > movedLayer.scale * 1.1f && abs(actual.rotation - movedLayer.rotation) > 10f
                    }
                    assertEquals(moved.layers.filterNot { it.id == layer.id }, transformed.layers.filterNot { it.id == layer.id })
                    tapDock("Undo")
                    persisted(store, seeded.id) { it.layers == moved.layers }
                    tapDock("Redo")
                    persisted(store, seeded.id) { it.layers == transformed.layers }
                    expected = transformed.layers
                }
            }
        } finally {
            runBlocking { PosterDatabase.get(context).posters().delete(draft.id) }
            imported.forEach { store.asset(it).delete() }
            original.delete()
        }
    }

    @Test
    fun offlineCutoutPreviewCanBeComparedRefinedAppliedAndUndone() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val store = ProjectStore(context)
        val source = store.newAsset()
        val template = templates(PosterMode.VOTING).first().create(PosterMode.VOTING)
        val slot = template.layers.first { it.slot == "A" }
        val fixture = template.updateLayer(slot.id) { it.copy(asset = source.name) }.copy(title = "Mask refinement fixture")
        var refined: String? = null
        try {
            instrumentation.context.assets.open("astronaut.jpg").use { input ->
                source.outputStream().use { input.copyTo(it) }
            }
            runBlocking { store.save(fixture) }
            ActivityScenario.launch(MainActivity::class.java).use {
                awaitText("Create a poster")
                tap("My Designs")
                tap(fixture.title)
                val dock = bounds("Editor tools")
                tap("A")
                val pinned = Rect().also { awaitText("Back").getBoundsInScreen(it) }
                assertEquals(dock, bounds("Editor tools"))
                tapDock("Remove BG")
                assertEquals(dock, bounds("Editor tools"))
                assertEquals(pinned, Rect().also { awaitText("Back").getBoundsInScreen(it) })
                assertTrue(bounds("Editor panel").bottom <= dock.top)
                assertTrue(bounds("Poster canvas").bottom <= bounds("Editor panel").top)
                capture("photo-dock.png")
                tap("Auto remove")
                awaitText("Preview background removal")
                tap("Show original")
                awaitText("Show cutout")
                tap("Show cutout")
                tap("Refine edges")
                touch(awaitNode("Cutout brush canvas") {
                    it.contentDescription?.toString() == "Cutout brush canvas"
                }, "cutout brush canvas")
                capture("mask-refinement.png")
                tapInPanel("Done")
                awaitText("Preview background removal")
                tap("Apply")
                val applied = persisted(store, fixture.id) { it.layers.first { layer -> layer.id == slot.id }.cutout != null }
                val appliedSlot = applied.layers.first { it.id == slot.id }
                refined = appliedSlot.cutout
                assertEquals(source.name, appliedSlot.asset)
                assertEquals(fixture.layers.filterNot { it.id == slot.id }, applied.layers.filterNot { it.id == slot.id })
                tapDock("Undo")
                persisted(store, fixture.id) { it.layers == fixture.layers }
            }
        } finally {
            runBlocking { PosterDatabase.get(context).posters().delete(fixture.id) }
            source.delete()
            refined?.let { store.asset(it).delete() }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
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

                                if (index == 0) tapDock(requireNotNull(slot.slot)) else tapDock("Replace photo")
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
                        back()
                        persisted(store, draft.id) { it.selectedId == null }
                    }
                    tap("Home")
                }
            }
        } finally {
            runBlocking { drafts.forEach { PosterDatabase.get(context).posters().delete(it.id) } }
            imported.forEach { store.asset(it).delete() }
        }
    }

    private fun pinchAndRotate(centerX: Int, centerY: Int) {
        val downTime = SystemClock.uptimeMillis()
        val properties = Array(2) { index ->
            MotionEvent.PointerProperties().apply {
                id = index
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        fun send(action: Int, fingers: Int, radius: Float, angle: Double) {
            val coordinates = Array(fingers) { index ->
                val direction = if (index == 0) -1 else 1
                MotionEvent.PointerCoords().apply {
                    x = centerX + direction * radius * cos(angle).toFloat()
                    y = centerY + direction * radius * sin(angle).toFloat()
                    pressure = 1f
                    size = .1f
                }
            }
            val event = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), action, fingers,
                properties.copyOfRange(0, fingers), coordinates, 0, 0,
                1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
            )
            try {
                assertTrue("Android rejected touch action $action", automation.injectInputEvent(event, true))
            } finally { event.recycle() }
            SystemClock.sleep(25)
        }
        val indexOne = 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT
        send(MotionEvent.ACTION_DOWN, 1, 16f, 0.0)
        send(MotionEvent.ACTION_POINTER_DOWN or indexOne, 2, 16f, 0.0)
        for (step in 1..8) {
            send(MotionEvent.ACTION_MOVE, 2, 16f + step, Math.toRadians(step * 5.0))
        }
        send(MotionEvent.ACTION_POINTER_UP or indexOne, 2, 24f, Math.toRadians(40.0))
        send(MotionEvent.ACTION_UP, 1, 24f, Math.toRadians(40.0))
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
        capture("navigation-failure.png")
        val visible = nodes().filter { it.isVisibleToUser }
            .map { "${it.packageName}: ${(it.text ?: it.contentDescription)?.toString().orEmpty()}" }
            .distinct().take(25)
        error("Text not visible: $label. Visible nodes: $visible")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        automation.takeScreenshot()?.let { screenshot ->
            try {
                File(context.cacheDir, name).outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally { screenshot.recycle() }
        }
    }

    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 8_000
        do {
            if (condition()) return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        error(message)
    }

    private fun bounds(description: String): Rect = Rect().also { rect ->
        awaitNode(description) { it.contentDescription?.toString() == description }.getBoundsInScreen(rect)
    }

    private fun tapDock(label: String) {
        val dock = bounds("Editor tools")
        fun visible() = nodes().firstOrNull { node ->
            val rect = Rect().also(node::getBoundsInScreen)
            node.isVisibleToUser && hasLabel(node, label) && dock.contains(rect.centerX(), rect.centerY()) && rect.width() >= 12
        }
        for (forward in listOf(true, false)) {
            repeat(8) {
                visible()?.let { touch(it, label); return }
                val start = dock.left + dock.width() * 4 / 5
                val end = dock.left + dock.width() / 4
                val y = dock.centerY()
                if (forward) shell("input swipe $start $y $end $y 230") else shell("input swipe $end $y $start $y 230")
                SystemClock.sleep(180)
            }
        }
        error("Dock action unavailable: $label")
    }

    private fun tapInPanel(label: String) {
        val rect = bounds("Editor panel")
        repeat(7) {
            nodes().firstOrNull { node ->
                val hit = Rect().also(node::getBoundsInScreen)
                node.isVisibleToUser && hasLabel(node, label) && rect.contains(hit.centerX(), hit.centerY())
            }?.let { touch(it, label); return }
            // Scroll along the panel edge, outside the mask drawing surface.
            shell("input swipe ${rect.left + 3} ${rect.bottom - 8} ${rect.left + 3} ${rect.top + 8} 280")
            SystemClock.sleep(180)
        }
        error("Panel action unavailable: $label")
    }

    private fun tap(label: String) {
        val findText = { nodes().firstOrNull { it.isVisibleToUser && hasLabel(it, label) } }
        fun waitForTarget(): AccessibilityNodeInfo? {
            val deadline = SystemClock.uptimeMillis() + 750
            do {
                findText()?.let { return it }
                SystemClock.sleep(50)
            } while (SystemClock.uptimeMillis() < deadline)
            return null
        }
        var candidate = waitForTarget()
        repeat(3) {
            if (candidate == null) {
                val scroll = nodes().firstOrNull { it.isVisibleToUser && it.isScrollable }
                if (scroll?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true) {
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    candidate = waitForTarget()
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
