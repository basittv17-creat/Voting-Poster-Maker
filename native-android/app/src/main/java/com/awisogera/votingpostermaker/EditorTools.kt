package com.awisogera.votingpostermaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val swatches = listOf(
    0xffef67d2, 0xff111827, 0xffffffff, 0xff7dd3fc, 0xfffda4af, 0xfffde68a, 0xff86efac,
    0xffc4b5fd, 0xfffb7185, 0xff38bdf8, 0xfff5f5f5, 0xff222222, 0xfff97316, 0xff22c55e, 0xff8b5cf6,
)

@Composable
fun BackgroundTools(apply: (Backdrop) -> Unit) {
    ToolRow {
        Tool("Transparent") { apply(Backdrop()) }
        listOf(
            Triple("Blue Neon", 0xff101c46, 0xff126eae), Triple("Red Fire", 0xff340919, 0xffbd3c14),
            Triple("Purple Neon", 0xff1d0845, 0xff7925bf), Triple("Premium Gold", 0xff211511, 0xffa3752d),
            Triple("Green and Gold", 0xff052e26, 0xff906e19), Triple("Dark Premium", 0xff080d1b, 0xff2b3e68),
        ).forEach { (name, start, end) ->
            FilledTonalButton(onClick = { apply(Backdrop(start, end)) }) { Text(name) }
        }
    }
    Swatches { apply(Backdrop(it)) }
}

@Composable
private fun Swatches(apply: (Long) -> Unit) = ToolRow {
    swatches.forEach { color ->
        val hex = color.toString(16).takeLast(6).uppercase()
        Button(onClick = { apply(color) }, colors = ButtonDefaults.buttonColors(
            containerColor = Color(color), contentColor = if (color == 0xff111827 || color == 0xff222222) Color.White else Color.Black,
        ), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("#$hex") }
    }
}

@Composable
fun LayerTools(layer: PosterLayer, vm: EditorViewModel, pick: (String) -> Unit, panel: (String) -> Unit) {
    fun update(change: (PosterLayer) -> PosterLayer) = vm.layer(layer.id, change)
    if (layer.locked) {
        Tool("Unlock") { update { it.copy(locked = false) } }
        return
    }
    var name by remember(layer.id, layer.name) { mutableStateOf(layer.name) }
    if (layer.kind == LayerKind.PHOTO) {
        ToolRow {
            Tool(if (layer.asset == null) "Add photo" else "Replace photo") { pick(layer.id) }
            if (layer.asset != null) Tool("Remove Background") { vm.segment(layer) }
            if (layer.cutout != null) Tool("Restore Original") { update { it.copy(cutout = null) } }
            Tool("Change Background") { panel("Photo Background") }
            Tool("Flip H") { update { it.copy(flipX = !it.flipX) } }
            Tool("Flip V") { update { it.copy(flipY = !it.flipY) } }
        }
        if (layer.asset != null) {
            ToolRow {
                Tool("B&W") { update { it.copy(saturation = 0f) } }
                Tool("Warm") { update { it.copy(warmth = 30f) } }
                Tool("Vignette") { update { it.copy(vignette = !it.vignette) } }
                Tool("Reset filters") { update { it.copy(saturation = 1f, contrast = 1f, warmth = 0f, brightness = 0f, vignette = false) } }
            }
            Adjustment("Crop zoom", layer.cropZoom, 1f..4f) { value -> update { it.copy(cropZoom = value) } }
            Adjustment("Crop left / right", layer.cropX, -1f..1f) { value -> update { it.copy(cropX = value) } }
            Adjustment("Crop up / down", layer.cropY, -1f..1f) { value -> update { it.copy(cropY = value) } }
            Adjustment("Brightness", layer.brightness, -90f..90f) { value -> update { it.copy(brightness = value) } }
            Adjustment("Contrast", layer.contrast, .2f..2f) { value -> update { it.copy(contrast = value) } }
            Adjustment("Saturation", layer.saturation, 0f..2f) { value -> update { it.copy(saturation = value) } }
            Adjustment("Warmth", layer.warmth, -55f..55f) { value -> update { it.copy(warmth = value) } }
        }
    }
    if (layer.kind == LayerKind.TEXT || layer.kind == LayerKind.VS) {
        var text by remember(layer.id, layer.text) { mutableStateOf(layer.text) }
        OutlinedTextField(value = text, onValueChange = { text = it.take(800) }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { update { it.copy(text = text) } }, enabled = text != layer.text) { Text("Apply text") }
        ToolRow {
            Tool("Bold") { update { it.copy(bold = !it.bold) } }
            Tool("Italic") { update { it.copy(italic = !it.italic) } }
            Tool("Underline") { update { it.copy(underline = !it.underline) } }
            listOf("start", "center", "end").forEach { alignment ->
                Tool(alignment.replaceFirstChar(Char::uppercase)) { update { it.copy(textAlign = alignment) } }
            }
            listOf("Sans" to "sans-serif", "Serif" to "serif", "Mono" to "monospace").forEach { (title, font) ->
                Tool(title) { update { it.copy(font = font, fontAsset = null) } }
            }
            Tool("Import font") { pick("font:${layer.id}") }
        }
        Adjustment("Font size", layer.fontSize, 22f..220f) { value -> update { it.copy(fontSize = value) } }
        Adjustment("Line spacing", layer.lineSpacing, .8f..2f) { value -> update { it.copy(lineSpacing = value) } }
        ToolRow {
            legacyTextPresets.forEach { preset ->
                Tool(preset.title) { update(preset::applyTo) }
            }
            Tool("Clear text background") { update { it.copy(backdrop = Backdrop()) } }
        }
    }
    if (layer.kind in listOf(LayerKind.TEXT, LayerKind.SHAPE, LayerKind.VS)) {
        Text("Fill")
        Swatches { color -> update { it.copy(color = color) } }
    }
    Adjustment("Resize", layer.scale, .15f..6f) { value -> update { it.copy(scale = value) } }
    Adjustment("Rotation", layer.rotation, -360f..360f) { value -> update { it.copy(rotation = value) } }
    Adjustment("Opacity", layer.opacity, 0f..1f) { value -> update { it.copy(opacity = value) } }
    Adjustment("Stroke", layer.stroke, 0f..22f) { value -> update { it.copy(stroke = value) } }
    Adjustment("Shadow", layer.shadow, 0f..22f) { value -> update { it.copy(shadow = value) } }
    ToolRow {
        Tool("Reset transform") {
            update {
                val center = when (it.slot) { "A" -> 310f; "B" -> 890f; else -> 600f }
                it.copy(x = center, y = 430f, rotation = 0f, scale = 1f, cropZoom = 1f,
                    cropX = 0f, cropY = 0f, flipX = false, flipY = false)
            }
        }
        Tool("Duplicate") { vm.add(layer.duplicate()) }
        Tool("Copy layer") { vm.copyLayer(layer) }
        Tool("Forward") { vm.reorder(layer.id, 1) }
        Tool("Backward") { vm.reorder(layer.id, -1) }
        Tool("Lock") { update { it.copy(locked = true) } }
        Tool("Delete") { vm.remove(layer) }
    }
    OutlinedTextField(name, { name = it.take(80) }, label = { Text("Layer name") }, modifier = Modifier.fillMaxWidth())
    if (name.isNotBlank() && name.trim() != layer.name) Tool("Rename") { update { it.copy(name = name.trim()) } }
}

@Composable
private fun Adjustment(label: String, current: Float, limits: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    var value by remember(current) { mutableFloatStateOf(current) }
    Text("$label  ${(value * 100).roundToInt() / 100f}", style = MaterialTheme.typography.bodySmall)
    Slider(value = value, onValueChange = { value = it }, valueRange = limits, onValueChangeFinished = { change(value) })
}

@Composable
fun LayersPanel(document: PosterDocument, vm: EditorViewModel, select: () -> Unit) {
    document.layers.asReversed().forEach { layer ->
        ToolRow {
            FilterChip(document.selectedId == layer.id, { vm.select(layer.id); select() }, { Text(layer.name) })
            Tool(if (layer.visible) "Hide" else "Show") { vm.layer(layer.id) { it.copy(visible = !it.visible) } }
            Tool(if (layer.locked) "Unlock" else "Lock") { vm.layer(layer.id) { it.copy(locked = !it.locked) } }
            Tool("Front") { vm.reorder(layer.id, document.layers.size) }
            Tool("Back") { vm.reorder(layer.id, -document.layers.size) }
        }
    }
}

private val stickers = listOf("⭐","❤️","🔥","👍","🎉","🏆","✨","🌹","💯","👑","🎈","☀️","🍼","🍼💧","🤱")
private val emoji = listOf("😀","😂","😍","😎","🥰","😢","😡","🤍","❤️","💙","💚","💛","🤣","😇")

@Composable
fun ElementTools(vm: EditorViewModel) {
    ToolRow {
        listOf("Circle", "Rectangle", "Rounded", "Triangle", "Star", "Line", "Hexagon").forEach { shape ->
            Tool(shape) { vm.add(PosterLayer(kind = LayerKind.SHAPE, name = shape, width = 220f, height = 220f,
                style = shape, color = 0xff64e5fb)) }
        }
    }
    Text("Stickers")
    GlyphTools(vm, stickers, sticker = true)
    Text("Emoji")
    GlyphTools(vm, emoji, sticker = false)
    ToolRow {
        listOf("Green & Gold" to "green_gold", "Tagda Muqabla #1" to "tagda_muqabla").forEach { (name, asset) ->
            Tool(name) { vm.add(PosterLayer(kind = LayerKind.FRAME, name = name, style = asset,
                width = 1200f, height = 900f, locked = true)) }
        }
    }
}

@Composable
private fun GlyphTools(vm: EditorViewModel, glyphs: List<String>, sticker: Boolean) = ToolRow {
    glyphs.forEach { glyph ->
        Tool(glyph) { vm.add(legacyGlyph(glyph, sticker)) }
    }
}

private val vsStyles = listOf(
    Triple("Fire", 0xffff651f, 0xff60050f), Triple("Neon", 0xff6c38ff, 0xff190a54),
    Triple("Electric", 0xff36d5fa, 0xff12327d), Triple("Gold", 0xfff7cc67, 0xff81521c),
    Triple("Black & Gold", 0xff624820, 0xff07080d), Triple("Premium Shield", 0xff8e6c42, 0xff17244b),
    Triple("Lightning", 0xff6295ff, 0xff520e89), Triple("Red vs Blue", 0xffd11f43, 0xff1462da),
    Triple("Purple Neon", 0xffa93eee, 0xff5c136e), Triple("Classic Competition", 0xffc93964, 0xff421642),
    Triple("Modern Election", 0xff0a936d, 0xff12302c), Triple("Metallic Gold", 0xffb17834, 0xff412a18),
)

@Composable
fun VsTools(document: PosterDocument, vm: EditorViewModel) = ToolRow {
    vsStyles.forEach { (label, accent, shadow) ->
        OutlinedButton(onClick = {
            val existing = document.layers.lastOrNull { it.kind == LayerKind.VS }
            if (existing == null) vm.add(PosterLayer(kind = LayerKind.VS, name = "$label VS", text = "VS",
                style = label, width = 190f, height = 130f, bold = true, accent = accent, strokeColor = shadow))
            else {
                vm.layer(existing.id) { it.copy(name = "$label VS", style = label, accent = accent, strokeColor = shadow) }
                vm.select(existing.id)
            }
        }, contentPadding = PaddingValues(0.dp)) {
            Column(Modifier.background(Brush.linearGradient(listOf(Color(accent), Color(shadow))), RoundedCornerShape(18.dp)).padding(12.dp)) {
                Text("VS", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(label, color = Color.White)
            }
        }
    }
}

@Composable
fun CanvasTools(document: PosterDocument, vm: EditorViewModel) {
    var title by remember(document.id, document.title) { mutableStateOf(document.title) }
    OutlinedTextField(title, { title = it.take(100) }, label = { Text("Design name") }, modifier = Modifier.fillMaxWidth())
    Tool("Save name") { if (title.isNotBlank()) vm.edit { it.copy(title = title.trim()) } }
    ToolRow {
        legacyCanvasPresets.forEach { preset ->
            FilterChip(
                selected = document.width == preset.width && document.height == preset.height,
                onClick = { vm.edit { it.copy(width = preset.width, height = preset.height) } },
                label = { Text(preset.title) },
            )
        }
    }
}
