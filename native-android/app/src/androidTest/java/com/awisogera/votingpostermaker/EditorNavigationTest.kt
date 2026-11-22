package com.awisogera.votingpostermaker

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
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

    private fun nodes(): List<AccessibilityNodeInfo> = buildList {
        fun visit(node: AccessibilityNodeInfo) {
            add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        automation.rootInActiveWindow?.let(::visit)
    }

    private fun awaitText(label: String): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 8_000
        do {
            nodes().firstOrNull { it.isVisibleToUser && it.text?.toString() == label }?.let { return it }
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

    private fun back() {
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent KEYCODE_BACK")).use {
            it.readBytes()
        }
    }
}
