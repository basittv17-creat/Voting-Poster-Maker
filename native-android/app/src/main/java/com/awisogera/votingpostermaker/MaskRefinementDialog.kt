package com.awisogera.votingpostermaker

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun MaskRefinementDialog(preview: CutoutPreview, vm: EditorViewModel, close: () -> Unit) {
    var brush by remember(preview) { mutableStateOf<MaskBrush?>(null) }
    var error by remember(preview) { mutableStateOf<String?>(null) }
    var restoring by remember { mutableStateOf(false) }
    var radius by remember { mutableFloatStateOf(.04f) }
    var feather by remember { mutableFloatStateOf(.5f) }
    var revision by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(preview) {
        try {
            brush = withContext(Dispatchers.IO) {
                val original = requireNotNull(vm.renderer.load(preview.original, 1536))
                val cutout = requireNotNull(vm.renderer.load(preview.image, 1536))
                val edited = requireNotNull(cutout.copy(Bitmap.Config.ARGB_8888, true))
                cutout.recycle()
                MaskBrush(original, edited)
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            error = "The cutout could not be opened for refinement."
        } catch (_: OutOfMemoryError) {
            error = "This device has too little memory to refine this cutout."
        }
    }

    Dialog(onDismissRequest = { if (!saving) close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Refine cutout", style = MaterialTheme.typography.titleLarge)
                val activeBrush = brush
                if (activeBrush != null) {
                    ToolRow {
                        FilterChip(!restoring, { restoring = false }, label = { Text("Erase") })
                        FilterChip(restoring, { restoring = true }, label = { Text("Restore") })
                    }
                    val bitmap = activeBrush.edited
                    val image = remember(bitmap) { bitmap.asImageBitmap() }
                    Canvas(
                        Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray)
                            .semantics { contentDescription = "Cutout brush canvas" }
                            .pointerInput(activeBrush, radius, feather, restoring, saving) {
                                if (saving) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val scale = minOf(size.width.toFloat() / bitmap.width, size.height.toFloat() / bitmap.height)
                                    if (scale <= 0f) return@awaitEachGesture
                                    val origin = Offset((size.width - bitmap.width * scale) / 2f, (size.height - bitmap.height * scale) / 2f)
                                    val brushRadius = (maxOf(bitmap.width, bitmap.height) * radius).coerceAtLeast(1f)
                                    var previous = (down.position - origin) / scale
                                    activeBrush.dab(previous.x, previous.y, brushRadius, feather, restoring)
                                    revision++
                                    down.consume()
                                    do {
                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) break
                                        val next = (change.position - origin) / scale
                                        if (next != previous) {
                                            activeBrush.stroke(next.x, next.y, previous.x, previous.y, brushRadius, feather, restoring)
                                            revision++
                                            previous = next
                                        }
                                        change.consume()
                                    } while (true)
                                }
                            },
                    ) {
                        if (revision < 0) return@Canvas
                        val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
                        val width = (bitmap.width * scale).roundToInt()
                        val height = (bitmap.height * scale).roundToInt()
                        val left = ((size.width - width) / 2).roundToInt()
                        val top = ((size.height - height) / 2).roundToInt()
                        drawRect(Color(0xff64748b), Offset(left.toFloat(), top.toFloat()), Size(width.toFloat(), height.toFloat()))
                        drawImage(image, dstOffset = IntOffset(left, top), dstSize = IntSize(width, height))
                    }
                    Text("Brush size")
                    Slider(radius, { radius = it }, enabled = !saving, valueRange = .012f..0.12f)
                    Text("Edge feather")
                    Slider(feather, { feather = it }, enabled = !saving)
                } else {
                    Spacer(Modifier.weight(1f))
                    if (error == null) CircularProgressIndicator()
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ToolRow {
                    Button(onClick = {
                        if (activeBrush != null) {
                            saving = true
                            scope.launch {
                                try {
                                    vm.replaceCutoutPreview(preview, activeBrush.edited)
                                    close()
                                } catch (failure: CancellationException) {
                                    throw failure
                                } catch (_: Exception) {
                                    error = "Could not save the refined preview."
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    }, enabled = activeBrush != null && revision > 0 && !saving) { Text("Done") }
                    TextButton(onClick = close, enabled = !saving) { Text("Discard brush edits") }
                }
            }
        }
    }
}
