package com.awisogera.votingpostermaker

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

@Serializable
enum class PosterMode(val title: String) { VOTING("Voting Poster"), SINGLE("Single Poster Design") }

@Serializable
enum class LayerKind { PHOTO, TEXT, SHAPE, STICKER, FRAME, VS }

@Serializable
data class Backdrop(val top: Long = 0, val bottom: Long = top, val image: String? = null)

@Serializable
data class PosterLayer(
    val id: String = UUID.randomUUID().toString(),
    val kind: LayerKind,
    val name: String,
    val x: Float = 600f,
    val y: Float = 450f,
    val width: Float = 400f,
    val height: Float = 200f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val locked: Boolean = false,
    val visible: Boolean = true,
    val slot: String? = null,
    val asset: String? = null,
    val cutout: String? = null,
    val backdrop: Backdrop = Backdrop(),
    val text: String = "",
    val color: Long = 0xffffffff,
    val accent: Long = 0,
    val style: String = "",
    val font: String = "sans-serif",
    val fontAsset: String? = null,
    val fontSize: Float = 70f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val textAlign: String = "center",
    val lineSpacing: Float = 1f,
    val stroke: Float = 0f,
    val strokeColor: Long = 0xff080d1b,
    val shadow: Float = 0f,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val cropZoom: Float = 1f,
    val cropX: Float = 0f,
    val cropY: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val vignette: Boolean = false,
    val shadowColor: Long? = null,
) {
    /** Restore a photo's visible framing without deleting its original, mask or backdrop. */
    fun resetPhotoTransform(): PosterLayer = copy(
        scale = 1f, rotation = 0f, flipX = false, flipY = false,
        cropZoom = 1f, cropX = 0f, cropY = 0f,
    )

    fun duplicate(): PosterLayer = copy(
        id = UUID.randomUUID().toString(),
        slot = null,
        name = "$name copy",
        x = (x + 30f).coerceAtMost(1200f),
        y = (y + 30f).coerceAtMost(900f),
    )

    fun contains(px: Float, py: Float): Boolean {
        val angle = Math.toRadians(-rotation.toDouble())
        val dx = px - x
        val dy = py - y
        return kotlin.math.abs(dx * cos(angle) - dy * sin(angle)) <= width * scale / 2 &&
            kotlin.math.abs(dx * sin(angle) + dy * cos(angle)) <= height * scale / 2
    }
}

@Serializable
data class PosterDocument(
    val mode: PosterMode,
    val id: String = UUID.randomUUID().toString(),
    val title: String = mode.title,
    val width: Int = 1200,
    val height: Int = 900,
    val background: Backdrop = Backdrop(0xff101d42, 0xff29104d),
    val layers: List<PosterLayer> = emptyList(),
    val selectedId: String? = null,
) {
    val selected get() = layers.find { it.id == selectedId }
    fun hit(x: Float, y: Float): PosterLayer? =
        layers.asReversed().firstOrNull { it.visible && !it.locked && it.kind != LayerKind.FRAME && it.contains(x, y) }

    fun updateLayer(id: String, change: (PosterLayer) -> PosterLayer) =
        copy(layers = layers.map { if (it.id == id) change(it) else it })

    /** Change the exact legacy frame without deleting either portrait or its edits.
     *  Keeps layer IDs, masks, text, stickers and other user-created objects intact. */
    fun switchVotingFrame(style: String?): PosterDocument {
        if (mode != PosterMode.VOTING || style !in setOf(null, "green_gold", "tagda_muqabla")) return this
        val previous = layers.firstOrNull { it.kind == LayerKind.FRAME &&
            it.style in setOf("green_gold", "tagda_muqabla") }?.style
        if (previous == style) return this
        val oldFramed = previous != null
        val newFramed = style != null
        val kept = layers.filterNot {
            (it.kind == LayerKind.FRAME && it.style in setOf("green_gold", "tagda_muqabla")) ||
                (it.kind == LayerKind.SHAPE && it.name == "Legacy divider")
        }
        val slots = kept.filter { it.kind == LayerKind.PHOTO && it.slot in listOf("A", "B") }.map { photo ->
            val oldX = if (photo.slot == "A") (if (oldFramed) 320f else 310f) else (if (oldFramed) 880f else 890f)
            val newX = if (photo.slot == "A") (if (newFramed) 320f else 310f) else (if (newFramed) 880f else 890f)
            photo.copy(x = photo.x + newX - oldX, y = photo.y + (if (newFramed) 410f else 430f) -
                (if (oldFramed) 410f else 430f), height = if (newFramed) 680f else 610f)
        }
        val other = kept.filterNot { it.kind == LayerKind.PHOTO && it.slot in listOf("A", "B") }
        val divider = if (!newFramed) emptyList() else listOf(
            PosterLayer(kind = LayerKind.SHAPE, name = "Legacy divider",
                x = 590f, y = 410f, width = 7f, height = 680f, locked = true)
        )
        val frame = when (style) {
            "green_gold" -> listOf(PosterLayer(kind = LayerKind.FRAME, name = "Green & Gold",
                style = style, width = 1200f, height = 900f, locked = true))
            "tagda_muqabla" -> listOf(PosterLayer(kind = LayerKind.FRAME, name = "Tagda Muqabla #1",
                style = style, width = 1200f, height = 900f, locked = true))
            else -> emptyList()
        }
        return copy(layers = slots + divider + frame + other)
    }

    /** Mirrors the original A/B Reset action: re-centre the photo in its slot,
     *  while keeping the imported original, cutout and backdrop intact.
     *  Advanced native flips/crop/rotation are reset as well. */
    fun resetPhotoToSlot(id: String): PosterDocument {
        val usesLegacyFrame = layers.any { it.kind == LayerKind.FRAME }
        return updateLayer(id) { layer ->
            if (layer.kind != LayerKind.PHOTO) layer else {
                val originalCenter = when (layer.slot) {
                    "A" -> (if (usesLegacyFrame) 320f else 310f) to (if (usesLegacyFrame) 410f else 430f)
                    "B" -> (if (usesLegacyFrame) 880f else 890f) to (if (usesLegacyFrame) 410f else 430f)
                    "Primary" -> 600f to 430f
                    else -> layer.x to layer.y
                }
                layer.resetPhotoTransform().copy(x = originalCenter.first, y = originalCenter.second)
            }
        }
    }
}

data class PosterTemplate(val name: String, val background: Backdrop, val frame: String? = null, val title: String? = null) {
    fun create(mode: PosterMode): PosterDocument {
        val photos = if (mode == PosterMode.VOTING) listOf(
            photo("A", if (frame == null) 310f else 320f, 500f),
            photo("B", if (frame == null) 890f else 880f, 500f)
        ) else listOf(photo("Primary", 600f, 720f))
        val headline = title?.let {
            listOf(PosterLayer(kind = LayerKind.TEXT, name = "Headline", text = it, y = 805f,
                width = 1000f, height = 150f, bold = true))
        }.orEmpty()
        val frameLayer = frame?.let {
            listOf(PosterLayer(kind = LayerKind.FRAME, name = name, style = it,
                width = 1200f, height = 900f, locked = true))
        }.orEmpty()
        val divider = if (frame == null) emptyList() else listOf(
            PosterLayer(kind = LayerKind.SHAPE, name = "Legacy divider", x = 590f, y = 410f,
                width = 7f, height = 680f, locked = true)
        )
        return PosterDocument(mode, title = name, background = background, layers = photos + divider + frameLayer + headline)
    }

    private fun photo(slot: String, x: Float, width: Float) =
        PosterLayer(kind = LayerKind.PHOTO, name = "$slot photo", x = x, y = if (frame == null) 430f else 410f,
            width = width, height = if (frame == null) 610f else 680f, slot = slot)
}

fun templates(mode: PosterMode): List<PosterTemplate> = if (mode == PosterMode.VOTING) listOf(
    PosterTemplate("Blue vs Purple", Backdrop(0xff101d42, 0xff29104d)),
    PosterTemplate("Green & Gold", Backdrop(0xff092b20, 0xff726019), "green_gold"),
    PosterTemplate("Tagda Muqabla #1", Backdrop(0xffef67d2), "tagda_muqabla"),
    PosterTemplate("Red vs Blue", Backdrop(0xff521019, 0xff143d88), title = "CAST YOUR VOTE"),
    // Original offline color themes; the legacy frame and palettes remain available.
    PosterTemplate("Teal Community Vote", Backdrop(0xff064e4e, 0xff0f766e), title = "YOUR VOICE MATTERS"),
    PosterTemplate("Copper vs Navy", Backdrop(0xff9c5b27, 0xff152747), title = "THE CHOICE IS YOURS"),
) else listOf(
    PosterTemplate("Midnight Portrait", Backdrop(0xff101d42, 0xff29104d)),
    PosterTemplate("Gold Campaign", Backdrop(0xff17101c, 0xff74541c), title = "YOUR CANDIDATE"),
    PosterTemplate("Electric Announcement", Backdrop(0xff13083a, 0xff1455a7), title = "COMING SOON"),
    PosterTemplate("Emerald Event", Backdrop(0xff063e37, 0xff24734b), title = "JOIN US"),
)
