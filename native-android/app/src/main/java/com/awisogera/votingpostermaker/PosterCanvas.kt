package com.awisogera.votingpostermaker

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
fun PosterCanvas(
    document: PosterDocument,
    renderer: PosterRenderer,
    modifier: Modifier,
    vm: EditorViewModel? = null,
    zoom: Float = 1f,
    pan: Offset = Offset.Zero,
    moveCanvas: Boolean = false,
    showGrid: Boolean = false,
    inputEnabled: Boolean = true,
    checkerboardLayer: String? = null,
    onViewport: (Float, Offset) -> Unit = { _, _ -> },
    onSelect: (PosterLayer?, Boolean) -> Unit = { _, _ -> },
) {
    val keys = renderer.keys(document)
    val images by produceState<Map<String, Bitmap>>(emptyMap(), keys, renderer) {
        val existing = value
        value = withContext(Dispatchers.IO) {
            val maxEdge = min(900, kotlin.math.sqrt(5_000_000.0 / keys.size.coerceAtLeast(1)).toInt())
            keys.mapNotNull { key ->
                (existing[key] ?: runCatching { renderer.load(key, maxEdge) }.getOrNull())?.let { key to it }
            }.toMap()
        }
    }
    val liveDocument by rememberUpdatedState(document)
    val notify by rememberUpdatedState(onSelect)
    val updateViewport by rememberUpdatedState(onViewport)
    val liveZoom by rememberUpdatedState(zoom)
    val livePan by rememberUpdatedState(pan)
    val gestures = if (vm == null || !inputEnabled) Modifier else Modifier.pointerInput(document.id, document.width, document.height, moveCanvas) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val ratio = min(size.width.toFloat() / document.width, size.height.toFloat() / document.height)
            if (ratio <= 0) return@awaitEachGesture
            val width = document.width * ratio
            val height = document.height * ratio
            val scaledWidth = width * liveZoom
            val scaledHeight = height * liveZoom
            val left = (size.width - scaledWidth) / 2 + livePan.x
            val top = (size.height - scaledHeight) / 2 + livePan.y
            val point = Offset((down.position.x - left) * 1200 / scaledWidth,
                (down.position.y - top) * 900 / scaledHeight)
            val onPoster = point.x in 0f..1200f && point.y in 0f..900f
            val layer = if (!moveCanvas && onPoster) liveDocument.hit(point.x, point.y) else null
            if (!moveCanvas) {
                vm.select(layer?.id)
                notify(layer, false)
            }
            var moved = false
            var travel = Offset.Zero
            var gestureZoom = liveZoom
            var gesturePan = livePan
            vm.startGesture()
            try {
                do {
                    val event = awaitPointerEvent()
                    val pan = event.calculatePan()
                    val zoom = event.calculateZoom()
                    val rotation = event.calculateRotation()
                    travel += pan
                    if (!moved && (travel.getDistance() > viewConfiguration.touchSlop ||
                                kotlin.math.abs(1f - zoom) > .025f || kotlin.math.abs(rotation) > 3f)) moved = true
                    if (moved && moveCanvas) {
                        gestureZoom = (gestureZoom * zoom).coerceIn(1f, 4f)
                        val nextPan = gesturePan + pan
                        gesturePan = Offset(
                            nextPan.x.coerceIn(-width * gestureZoom / 2, width * gestureZoom / 2),
                            nextPan.y.coerceIn(-height * gestureZoom / 2, height * gestureZoom / 2),
                        )
                        updateViewport(gestureZoom, gesturePan)
                    } else if (moved && layer != null) {
                        vm.transform(layer.id, pan.x * 1200 / scaledWidth, pan.y * 900 / scaledHeight, zoom, rotation)
                    }
                    if (moved && (moveCanvas || layer != null)) {
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                vm.endGesture()
            }
            if (!moved && !moveCanvas) notify(layer, true)
        }
    }
    val accessibility = if (vm == null) Modifier else Modifier.semantics { contentDescription = "Poster canvas" }
    Canvas(modifier.then(gestures).then(accessibility)) {
        val ratio = min(size.width / document.width, size.height / document.height)
        val width = ratio * document.width * zoom
        val height = ratio * document.height * zoom
        val left = (size.width - width) / 2 + pan.x
        val top = (size.height - height) / 2 + pan.y
        drawRect(Color(0xff0e1528), Offset(left, top),
            androidx.compose.ui.geometry.Size(width, height))
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val save = native.save()
            native.clipRect(0f, 0f, size.width, size.height)
            native.translate(left, top)
            native.scale(width / 1200f, height / 900f)
            renderer.draw(native, document, images, editing = true, checkerboardLayer = checkerboardLayer)
            if (showGrid) {
                val paint = android.graphics.Paint().apply { color = 0x75ffffff; strokeWidth = 2f }
                for (x in listOf(300f, 600f, 900f)) native.drawLine(x, 0f, x, 900f, paint)
                for (y in listOf(225f, 450f, 675f)) native.drawLine(0f, y, 1200f, y, paint)
            }
            native.restoreToCount(save)
        }
    }
}
