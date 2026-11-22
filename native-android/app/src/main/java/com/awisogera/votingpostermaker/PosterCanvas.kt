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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
fun PosterCanvas(
    document: PosterDocument,
    renderer: PosterRenderer,
    modifier: Modifier,
    vm: EditorViewModel? = null,
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
    val gestures = if (vm == null) Modifier else Modifier.pointerInput(document.id, document.width, document.height) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val ratio = min(size.width.toFloat() / document.width, size.height.toFloat() / document.height)
            if (ratio <= 0) return@awaitEachGesture
            val width = document.width * ratio
            val height = document.height * ratio
            val left = (size.width - width) / 2
            val top = (size.height - height) / 2
            val point = Offset((down.position.x - left) * 1200 / width, (down.position.y - top) * 900 / height)
            val layer = liveDocument.hit(point.x, point.y)
            vm.select(layer?.id)
            notify(layer, false)
            var moved = false
            var travel = Offset.Zero
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
                    if (moved && layer != null) {
                        vm.transform(layer.id, pan.x * 1200 / width, pan.y * 900 / height, zoom, rotation)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                vm.endGesture()
            }
            if (!moved) notify(layer, true)
        }
    }
    Canvas(modifier.then(gestures)) {
        val ratio = min(size.width / document.width, size.height / document.height)
        val width = ratio * document.width
        val height = ratio * document.height
        drawRect(Color(0xff0e1528), Offset((size.width - width) / 2, (size.height - height) / 2),
            androidx.compose.ui.geometry.Size(width, height))
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val save = native.save()
            native.translate((size.width - width) / 2, (size.height - height) / 2)
            native.scale(width / 1200f, height / 900f)
            renderer.draw(native, document, images, editing = true)
            native.restoreToCount(save)
        }
    }
}
