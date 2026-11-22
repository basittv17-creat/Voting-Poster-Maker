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

    fun dab(x: Float, y: Float, radius: Float, feather: Float, restore: Boolean) {
        val left = floor(x - radius).toInt().coerceIn(0, edited.width)
        val top = floor(y - radius).toInt().coerceIn(0, edited.height)
        val right = ceil(x + radius).toInt().coerceIn(left, edited.width)
        val bottom = ceil(y + radius).toInt().coerceIn(top, edited.height)
        val width = right - left
        val height = bottom - top
        if (width == 0 || height == 0) return
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
