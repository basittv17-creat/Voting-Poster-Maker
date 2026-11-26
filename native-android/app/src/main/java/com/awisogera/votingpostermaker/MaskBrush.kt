package com.awisogera.votingpostermaker

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class MaskBrush(private val original: Bitmap, val edited: Bitmap) {
    init {
        require(original.width == edited.width && original.height == edited.height && edited.isMutable)
    }

    private var pixels = IntArray(0)
    private var source = IntArray(0)

    // Save pixels only for the tiles touched by this stroke. Swapping a patch
    // with the bitmap also produces the inverse patch used by redo.
    private data class Tile(val x: Int, val y: Int, val width: Int, val height: Int, val before: IntArray)
    private val undo = ArrayDeque<List<Tile>>()
    private val redo = ArrayDeque<List<Tile>>()
    private var strokeTiles: MutableMap<Int, Tile>? = null
    private var historyBytes = 0
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()

    fun beginStroke() { strokeTiles = mutableMapOf() }

    fun finishStroke() {
        val tiles = strokeTiles?.values?.toList().orEmpty()
        strokeTiles = null
        if (tiles.isEmpty()) return
        redo.forEach { historyBytes -= it.sumOf { tile -> tile.before.size * 4 } }
        redo.clear()
        undo.addLast(tiles)
        historyBytes += tiles.sumOf { it.before.size * 4 }
        while (undo.size > 1 && (historyBytes > 12 * 1024 * 1024 || undo.size > 60)) {
            historyBytes -= undo.removeFirst().sumOf { it.before.size * 4 }
        }
    }

    private fun rememberTiles(left: Int, top: Int, right: Int, bottom: Int) {
        val tiles = strokeTiles ?: return
        val side = 64
        for (ty in top / side..(bottom - 1) / side) for (tx in left / side..(right - 1) / side) {
            val x = tx * side
            val y = ty * side
            tiles.getOrPut(y * edited.width + x) {
                val w = minOf(side, edited.width - x)
                val h = minOf(side, edited.height - y)
                Tile(x, y, w, h, IntArray(w * h).also { edited.getPixels(it, 0, w, x, y, w, h) })
            }
        }
    }

    private fun swap(from: ArrayDeque<List<Tile>>, to: ArrayDeque<List<Tile>>) {
        val tiles = from.removeLastOrNull() ?: return
        for (tile in tiles) {
            val size = tile.before.size
            if (pixels.size < size) pixels = IntArray(size)
            edited.getPixels(pixels, 0, tile.width, tile.x, tile.y, tile.width, tile.height)
            edited.setPixels(tile.before, 0, tile.width, tile.x, tile.y, tile.width, tile.height)
            pixels.copyInto(tile.before, endIndex = size)
        }
        to.addLast(tiles)
    }

    fun undoStroke() = swap(undo, redo)
    fun redoStroke() = swap(redo, undo)

    fun dab(x: Float, y: Float, radius: Float, feather: Float, restore: Boolean) {
        val left = floor(x - radius).toInt().coerceIn(0, edited.width)
        val top = floor(y - radius).toInt().coerceIn(0, edited.height)
        val right = ceil(x + radius).toInt().coerceIn(left, edited.width)
        val bottom = ceil(y + radius).toInt().coerceIn(top, edited.height)
        val width = right - left
        val height = bottom - top
        if (width == 0 || height == 0) return
        rememberTiles(left, top, right, bottom)
        val count = width * height
        if (pixels.size < count) pixels = IntArray(count)
        if (restore && source.size < count) source = IntArray(count)
        edited.getPixels(pixels, 0, width, left, top, width, height)
        if (restore) original.getPixels(source, 0, width, left, top, width, height)
        val radiusSquared = radius * radius
        val solidRadius = radius * (1f - feather)
        val solidSquared = solidRadius * solidRadius
        for (index in 0 until count) {
            val dx = left + index % width + .5f - x
            val dy = top + index / width + .5f - y
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared >= radiusSquared) continue
            val strength = if (distanceSquared <= solidSquared) 1f else (radius - sqrt(distanceSquared)) / (radius * feather)
            val current = Color.alpha(pixels[index])
            val desired = if (restore) Color.alpha(source[index]) else 0
            val alpha = (current + (desired - current) * strength).roundToInt()
            val rgb = if (restore) source[index] else pixels[index]
            pixels[index] = (alpha shl 24) or (rgb and 0xffffff)
        }
        edited.setPixels(pixels, 0, width, left, top, width, height)
    }

    fun stroke(x: Float, y: Float, previousX: Float, previousY: Float, radius: Float, feather: Float, restore: Boolean) {
        val dx = x - previousX
        val dy = y - previousY
        val steps = ceil(hypot(dx, dy) / (radius / 3f)).toInt().coerceAtLeast(1)
        for (step in 1..steps) {
            val fraction = step.toFloat() / steps
            dab(previousX + dx * fraction, previousY + dy * fraction, radius, feather, restore)
        }
    }
}
