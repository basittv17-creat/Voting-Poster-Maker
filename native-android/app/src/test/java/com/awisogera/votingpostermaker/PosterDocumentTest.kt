package com.awisogera.votingpostermaker

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PosterDocumentTest {
    @Test fun changingALeavesBIntact() {
        val document = templates(PosterMode.VOTING).first().create(PosterMode.VOTING)
        val (a, b) = document.layers
        val updated = document.updateLayer(a.id) {
            it.copy(asset = "a-original", cutout = "a-cutout", backdrop = Backdrop(0xffa00000))
        }
        assertEquals(b, updated.layers[1])
        assertEquals("a-cutout", updated.layers[0].cutout)
        assertNotEquals(document.background, updated.layers[0].backdrop)
    }

    @Test fun copiedPhotoRetainsEditsWithoutTakingTheOriginalSlot() {
        val original = PosterLayer(
            kind = LayerKind.PHOTO, name = "A photo", slot = "A",
            asset = "portrait", cutout = "mask", rotation = 15f, x = 1195f, y = 895f,
        )
        val duplicate = original.duplicate()
        assertNotEquals(original.id, duplicate.id)
        assertNull(duplicate.slot)
        assertEquals(original.asset, duplicate.asset)
        assertEquals(original.cutout, duplicate.cutout)
        assertEquals(original.rotation, duplicate.rotation, 0f)
        assertEquals(1200f, duplicate.x, 0f)
        assertEquals(900f, duplicate.y, 0f)
        assertEquals("A", original.slot)
    }

    @Test fun rotationAndTopmostOrderAffectHitTesting() {
        val base = PosterLayer(kind = LayerKind.TEXT, name = "Rotated", width = 300f, height = 80f, rotation = 90f)
        val top = base.copy(id = "top")
        val document = PosterDocument(PosterMode.SINGLE, layers = listOf(base, top))
        assertEquals(top, document.hit(600f, 575f))
        assertNull(document.hit(725f, 450f))
        assertEquals(base, document.copy(layers = listOf(base, top.copy(locked = true))).hit(600f, 575f))
    }

    @Test fun persistedDocumentRetainsUrduAndSelection() {
        val layer = PosterLayer(kind = LayerKind.TEXT, name = "Headline",
            text = "اپنا ووٹ ضرور ڈالیں\nپاکستان زندہ باد", rotation = 15f)
        val document = PosterDocument(PosterMode.SINGLE, selectedId = layer.id, layers = listOf(layer))
        assertEquals(document, Json.decodeFromString<PosterDocument>(Json.encodeToString(document)))
    }

    @Test fun legacyPresetsRetainOriginalCanvasAndSeparateOutlineGlow() {
        assertEquals(11, legacyCanvasPresets.size)
        assertEquals(CanvasPreset("1280 × 1100", 1280, 1100), legacyCanvasPresets.first())
        assertEquals(11, legacyCanvasPresets.map { it.width to it.height }.distinct().size)
        assertEquals(8, legacyTextPresets.size)
        val neon = legacyTextPresets.first { it.title == "Neon Purple" }
        val layer = PosterLayer(kind = LayerKind.TEXT, name = "Urdu", text = "ووٹ", fontAsset = "custom")
        val styled = neon.applyTo(layer)
        assertEquals(layer.id, styled.id)
        assertEquals(layer.text, styled.text)
        assertEquals(0xff28004b, styled.strokeColor)
        assertEquals(0xffed5cff, styled.shadowColor)
        assertNull(styled.fontAsset)
        assertEquals(styled, Json.decodeFromString<PosterLayer>(Json.encodeToString(styled)))
    }

}
