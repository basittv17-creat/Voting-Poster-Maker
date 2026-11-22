package com.awisogera.votingpostermaker

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.min

class PosterRenderer(private val context: Context, private val store: ProjectStore) {
    fun load(key: String, maxEdge: Int): Bitmap? {
        val bundled = key.startsWith("frame:")
        val path = if (bundled) "frames/${key.removePrefix("frame:")}.png" else ""
        fun decode(options: BitmapFactory.Options) =
            if (bundled) context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
            else BitmapFactory.decodeFile(store.asset(key).path, options)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val bitmap = decode(BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        if (bundled) return bitmap
        val orientation = runCatching { ExifInterface(store.asset(key)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        ) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            2 -> matrix.setScale(-1f, 1f)
            3 -> matrix.setRotate(180f)
            4 -> matrix.setScale(1f, -1f)
            5 -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            6 -> matrix.setRotate(90f)
            7 -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            8 -> matrix.setRotate(-90f)
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    fun keys(document: PosterDocument): List<String> = buildList {
        document.background.image?.let(::add)
        document.layers.filter { it.visible }.forEach { layer ->
            (layer.cutout ?: layer.asset)?.let(::add)
            layer.backdrop.image?.let(::add)
            if (layer.kind == LayerKind.FRAME) add("frame:${layer.style}")
        }
    }.distinct()

    fun draw(canvas: Canvas, document: PosterDocument, bitmaps: Map<String, Bitmap>, editing: Boolean) {
        val outer = canvas.save()
        val area = RectF(0f, 0f, 1200f, 900f)
        canvas.clipRect(area)
        backdrop(canvas, document.background, area, bitmaps)
        document.layers.filter { it.visible }.forEach { layer ->
            val save = canvas.save()
            canvas.translate(layer.x, layer.y)
            canvas.rotate(layer.rotation)
            canvas.scale(layer.scale, layer.scale)
            val box = RectF(-layer.width / 2, -layer.height / 2, layer.width / 2, layer.height / 2)
            val contents = if (layer.opacity < 1f) canvas.saveLayerAlpha(null, (255 * layer.opacity).toInt()) else canvas.save()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            when (layer.kind) {
                LayerKind.PHOTO -> {
                    if (layer.shadow > 0) {
                        paint.color = 0x65000000
                        canvas.drawRoundRect(RectF(box).apply { offset(8f, 12f) }, 16f, 16f, paint)
                    }
                    backdrop(canvas, layer.backdrop, box, bitmaps)
                    val bitmap = (layer.cutout ?: layer.asset)?.let { bitmaps[it] }
                    if (bitmap != null) {
                        val photo = canvas.save()
                        canvas.clipRect(box)
                        canvas.scale(if (layer.flipX) -1f else 1f, if (layer.flipY) -1f else 1f)
                        paint.color = Color.WHITE
                        paint.colorFilter = filter(layer)
                        cover(canvas, bitmap, box, paint, layer.cropZoom, contain = layer.cutout != null,
                            panX = layer.cropX, panY = layer.cropY)
                        paint.colorFilter = null
                        canvas.restoreToCount(photo)
                        if (layer.vignette) {
                            paint.shader = RadialGradient(0f, 0f, min(box.width(), box.height()) * .7f,
                                intArrayOf(0, 0, 0xa0000000.toInt()), floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP)
                            canvas.drawRect(box, paint)
                            paint.shader = null
                        }
                    } else if (editing) {
                        paint.color = 0x294abdfc
                        canvas.drawRoundRect(box, 24f, 24f, paint)
                        text(canvas, layer.copy(text = "Add ${layer.slot ?: "a"} photo", fontSize = 37f), box)
                    }
                    if (layer.stroke > 0) outline(canvas, box, layer.strokeColor.toInt(), layer.stroke)
                }
                LayerKind.TEXT, LayerKind.STICKER -> {
                    backdrop(canvas, layer.backdrop, box, bitmaps)
                    text(canvas, layer, box)
                }
                LayerKind.VS -> {
                    paint.shader = LinearGradient(box.left, box.top, box.right, box.bottom,
                        layer.accent.toInt(), layer.strokeColor.toInt(), Shader.TileMode.CLAMP)
                    canvas.drawRoundRect(box, 46f, 46f, paint)
                    outline(canvas, box, layer.accent.toInt(), 4f)
                    text(canvas, layer, box)
                }
                LayerKind.FRAME -> bitmaps["frame:${layer.style}"]?.let { canvas.drawBitmap(it, null, box, paint) }
                LayerKind.SHAPE -> shape(canvas, layer, box, paint)
            }
            canvas.restoreToCount(contents)
            if (editing && layer.id == document.selectedId) {
                outline(canvas, box, 0xff64e5fb.toInt(), 5f)
                paint.color = 0xff64e5fb.toInt()
                for (x in listOf(box.left, box.right)) for (y in listOf(box.top, box.bottom)) {
                    canvas.drawCircle(x, y, 10f, paint)
                }
            }
            canvas.restoreToCount(save)
        }
        canvas.restoreToCount(outer)
    }

    private fun text(canvas: Canvas, layer: PosterLayer, box: RectF) {
        val fontStyle = when {
            layer.bold && layer.italic -> Typeface.BOLD_ITALIC
            layer.bold -> Typeface.BOLD
            layer.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val font = layer.fontAsset?.let { runCatching { Typeface.createFromFile(store.asset(it)) }.getOrNull() }
            ?: Typeface.create(layer.font, Typeface.NORMAL)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(font, fontStyle)
            textSize = layer.fontSize
            color = layer.color.toInt()
            isUnderlineText = layer.underline
        }
        val layout = StaticLayout.Builder.obtain(layer.text, 0, layer.text.length, paint,
            (box.width() - 12).toInt().coerceAtLeast(1))
            .setAlignment(when (layer.textAlign) {
                "start" -> Layout.Alignment.ALIGN_NORMAL
                "end" -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_CENTER
            })
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setLineSpacing(0f, layer.lineSpacing)
            .build()
        val save = canvas.save()
        canvas.translate(box.left + 6f, -layout.height / 2f)
        if (layer.stroke > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = layer.stroke * 2
            paint.color = layer.strokeColor.toInt()
            layout.draw(canvas)
            paint.style = Paint.Style.FILL
            paint.color = layer.color.toInt()
        }
        if (layer.shadow > 0) paint.setShadowLayer(layer.shadow, 2f, 3f, (layer.shadowColor ?: layer.strokeColor).toInt())
        if (layer.accent != 0L && layer.kind == LayerKind.TEXT) {
            paint.shader = LinearGradient(0f, 0f, 0f, layout.height.toFloat().coerceAtLeast(1f),
                layer.color.toInt(), layer.accent.toInt(), Shader.TileMode.CLAMP)
        }
        layout.draw(canvas)
        canvas.restoreToCount(save)
    }

    private fun filter(layer: PosterLayer): ColorMatrixColorFilter {
        val matrix = ColorMatrix().apply { setSaturation(layer.saturation) }
        val c = layer.contrast
        val offset = (1f - c) * 128f + layer.brightness
        matrix.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, offset + layer.warmth,
            0f, c, 0f, 0f, offset,
            0f, 0f, c, 0f, offset - layer.warmth,
            0f, 0f, 0f, 1f, 0f,
        )))
        return ColorMatrixColorFilter(matrix)
    }

    private fun backdrop(canvas: Canvas, background: Backdrop, box: RectF, bitmaps: Map<String, Bitmap>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (background.top != background.bottom) {
            paint.shader = LinearGradient(box.left, box.top, box.right, box.bottom,
                background.top.toInt(), background.bottom.toInt(), Shader.TileMode.CLAMP)
        } else paint.color = background.top.toInt()
        canvas.drawRect(box, paint)
        paint.shader = null
        paint.color = Color.WHITE
        background.image?.let { bitmaps[it] }?.let { cover(canvas, it, box, paint) }
    }

    private fun cover(canvas: Canvas, bitmap: Bitmap, box: RectF, paint: Paint, zoom: Float = 1f,
        contain: Boolean = false, panX: Float = 0f, panY: Float = 0f,
    ) {
        val x = box.width() / bitmap.width
        val y = box.height() / bitmap.height
        val scale = (if (contain) min(x, y) else max(x, y)) * zoom
        val w = bitmap.width * scale / 2
        val h = bitmap.height * scale / 2
        val save = canvas.save()
        canvas.clipRect(box)
        val centerX = box.centerX() + panX * (w - box.width() / 2).coerceAtLeast(0f)
        val centerY = box.centerY() + panY * (h - box.height() / 2).coerceAtLeast(0f)
        canvas.drawBitmap(bitmap, null, RectF(centerX - w, centerY - h, centerX + w, centerY + h), paint)
        canvas.restoreToCount(save)
    }

    private fun outline(canvas: Canvas, box: RectF, color: Int, width: Float) {
        canvas.drawRect(box, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; strokeWidth = width; style = Paint.Style.STROKE
        })
    }

    private fun shape(canvas: Canvas, layer: PosterLayer, box: RectF, paint: Paint) {
        fun draw() = when (layer.style) {
            "Circle" -> canvas.drawOval(box, paint)
            "Rounded" -> canvas.drawRoundRect(box, 42f, 42f, paint)
            "Line" -> canvas.drawRect(box.left, -4f, box.right, 4f, paint)
            "Triangle", "Star", "Hexagon" -> {
                val points = when (layer.style) { "Triangle" -> 3; "Star" -> 10; else -> 6 }
                val path = Path()
                repeat(points) { index ->
                    val angle = 2 * Math.PI * index / points - Math.PI / 2
                    val inset = if (layer.style == "Star" && index % 2 != 0) .44f else 1f
                    val x = kotlin.math.cos(angle).toFloat() * box.width() / 2 * inset
                    val y = kotlin.math.sin(angle).toFloat() * box.height() / 2 * inset
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            else -> canvas.drawRect(box, paint)
        }
        paint.color = layer.color.toInt()
        draw()
        if (layer.stroke > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = layer.stroke
            paint.color = layer.strokeColor.toInt()
            draw()
        }
    }
}
