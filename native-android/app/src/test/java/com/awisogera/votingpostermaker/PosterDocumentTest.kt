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
}
