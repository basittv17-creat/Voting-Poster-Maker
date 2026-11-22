package com.awisogera.votingpostermaker

import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import kotlin.math.roundToInt
import org.junit.Test

class EditorNavigationTest {
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @Test
    fun eachModeOpensEditorAndBackClosesExportFirst() {
        ActivityScenario.launch(MainActivity::class.java).use {
            for ((mode, template) in listOf("Voting Poster" to "Blue vs Purple", "Single Poster Design" to "Midnight Portrait")) {
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
    fun touchingPhotosSelectsOnlyTheIntendedLayer() {
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
                tap("My Designs")
                tap(seeded.title)
                for (layer in seeded.layers) {
                    val bounds = Rect()
                    awaitNode("Poster canvas") { it.contentDescription?.toString() == "Poster canvas" }
                        .getBoundsInScreen(bounds)
                    val scale = minOf(bounds.width() / 1200f, bounds.height() / 900f)
                    val x = bounds.exactCenterX() + (layer.x - 600f) * scale
                    val y = bounds.exactCenterY() + (layer.y - 450f) * scale
                    shell("input tap ${x.roundToInt()} ${y.roundToInt()}")
                    awaitText(layer.name)
                    val saved = runBlocking {
                        withTimeout(8_000) {
                            store.projects.first { records ->
                                records.any { it.id == seeded.id && store.read(it).selectedId == layer.id }
                            }.first { it.id == seeded.id }.let(store::read)
                        }
                    }
                    assertEquals(seeded.layers, saved.layers)
                }
            }
        } finally {
            runBlocking { PosterDatabase.get(context).posters().delete(draft.id) }
            imported.forEach { store.asset(it).delete() }
            original.delete()
        }
    }

    private fun nodes(): List<AccessibilityNodeInfo> = buildList {
        fun visit(node: AccessibilityNodeInfo) {
            add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        automation.rootInActiveWindow?.let(::visit)
    }

    private fun awaitText(label: String): AccessibilityNodeInfo = awaitNode(label) {
        val text = it.text?.toString().orEmpty()
        text == label || text.startsWith("$label,") || text.startsWith("$label\n")
    }

    private fun awaitNode(label: String, matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 8_000
        do {
            nodes().firstOrNull { it.isVisibleToUser && matches(it) }?.let { return it }
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        error("Text not visible: $label")
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
        var node: AccessibilityNodeInfo? = awaitText(label)
        while (node != null && !node.isClickable) node = node.parent
        assertTrue("Could not tap $label", node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun back() = shell("input keyevent KEYCODE_BACK")

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
            it.readBytes()
        }
    }
}
