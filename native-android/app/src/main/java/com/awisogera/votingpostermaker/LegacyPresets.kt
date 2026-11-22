package com.awisogera.votingpostermaker

import kotlin.math.roundToInt

internal data class CanvasPreset(val title: String, val width: Int, val height: Int)

internal val legacyCanvasPresets = listOf(
    CanvasPreset("1280 × 1100", 1280, 1100),
    CanvasPreset("1:1 Square", 1080, 1080),
    CanvasPreset("4:3 Landscape", 1200, 900),
    CanvasPreset("3:4 Portrait", 900, 1200),
    CanvasPreset("16:9 Landscape", 1280, 720),
    CanvasPreset("9:16 Story / Reel", 720, 1280),
    CanvasPreset("3:2 Landscape", 1200, 800),
    CanvasPreset("2:3 Portrait", 800, 1200),
    CanvasPreset("5:4 Landscape", 1250, 1000),
    CanvasPreset("4:5 Portrait", 960, 1200),
    CanvasPreset("21:9 Wide", 1260, 540),
)

internal data class LegacyTextPreset(
    val title: String,
    val background: Backdrop,
    val fill: Long,
    val outline: Long,
    val glow: Long,
    val font: String,
) {
    fun applyTo(layer: PosterLayer): PosterLayer = layer.copy(
        backdrop = background,
        color = fill,
        accent = 0,
        strokeColor = outline,
        stroke = (layer.fontSize * .065f).roundToInt().coerceAtLeast(3) / 2f,
        shadow = 9f,
        shadowColor = glow,
        font = font,
        fontAsset = null,
        bold = true,
    )
}

internal val legacyTextPresets = listOf(
    LegacyTextPreset("Royal Gold", Backdrop(0xff3b2100, 0xfff8d35a), 0xfffff5bd, 0xff4b1600, 0xff120800, "serif"),
    LegacyTextPreset("Neon Purple", Backdrop(0xff16002e, 0xffa100ff), 0xffffffff, 0xff28004b, 0xffed5cff, "sans-serif-black"),
    LegacyTextPreset("Fire Red", Backdrop(0xff380000, 0xffff3b12), 0xfffff3d6, 0xff520000, 0xffff8a00, "sans-serif-condensed"),
    LegacyTextPreset("Ice Blue", Backdrop(0xff001b3b, 0xff00b9ff), 0xffffffff, 0xff002447, 0xff76eaff, "sans-serif-black"),
    LegacyTextPreset("Emerald", Backdrop(0xff002e1b, 0xff00a86b), 0xfffff4b8, 0xff003522, 0xff72ffbd, "serif"),
    LegacyTextPreset("Carbon Gold", Backdrop(0xff080808, 0xffb8860b), 0xffffe9a0, 0xff151515, 0xffffce42, "sans-serif-condensed"),
    LegacyTextPreset("Royal Red", Backdrop(0xff35000d, 0xffc40035), 0xffffffff, 0xff37000a, 0xffff4668, "serif"),
    LegacyTextPreset("Cyber", Backdrop(0xff001b21, 0xff00b8b8), 0xffe8ffff, 0xff00282d, 0xff00ffff, "sans-serif-black"),
)
