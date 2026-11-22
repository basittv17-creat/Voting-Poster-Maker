package com.awisogera.votingpostermaker

import android.app.Application
import android.graphics.Typeface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CutoutPreview(val documentId: String, val layerId: String, val original: String, val image: String)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProjectStore(app)
    val renderer = PosterRenderer(app, store)
    val exporter = PosterExport(app, renderer)
    val projects = store.projects
    private val _document = MutableStateFlow<PosterDocument?>(null)
    val document = _document.asStateFlow()
    val notice = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val cutout = MutableStateFlow<CutoutPreview?>(null)
    private val _clipboard = MutableStateFlow<PosterLayer?>(null)
    val clipboard = _clipboard.asStateFlow()

    fun copyLayer(layer: PosterLayer) { _clipboard.value = layer }
    fun pasteLayer() { _clipboard.value?.let { add(it.duplicate()) } }
    private var cutoutJob: Job? = null
    private val history = ArrayDeque<PosterDocument>()
    private val future = ArrayDeque<PosterDocument>()
    private val saves = Channel<PosterDocument>(Channel.UNLIMITED)
    private var gestureStart: PosterDocument? = null

    init {
        viewModelScope.launch {
            for (item in saves) {
                try {
                    withContext(Dispatchers.IO) { store.save(item) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    notice.value = "Autosave failed. Your current design remains available."
                }
            }
        }
    }

    private fun save(doc: PosterDocument) { saves.trySend(doc) }

    fun open(doc: PosterDocument) {
        cancelCutout()
        history.clear()
        future.clear()
        gestureStart = null
        _document.value = doc
        save(doc)
    }

    fun open(item: SavedPoster): Boolean = runCatching { open(store.read(item)) }.isSuccess.also {
        if (!it) notice.value = "This design could not be opened."
    }

    fun edit(change: (PosterDocument) -> PosterDocument) {
        val old = _document.value ?: return
        val next = change(old)
        if (old == next) return
        remember(old)
        _document.value = next
        save(next)
    }

    private fun remember(old: PosterDocument) {
        history.addLast(old)
        if (history.size > 60) history.removeFirst()
        future.clear()
    }

    fun select(id: String?) {
        val doc = _document.value ?: return
        if (id == doc.selectedId) return
        _document.value = doc.copy(selectedId = id).also(::save)
    }

    fun undo() {
        val current = _document.value ?: return
        val prior = history.removeLastOrNull() ?: return
        future.addLast(current)
        _document.value = prior
        save(prior)
    }

    fun redo() {
        val current = _document.value ?: return
        val next = future.removeLastOrNull() ?: return
        history.addLast(current)
        _document.value = next
        save(next)
    }

    fun layer(id: String, change: (PosterLayer) -> PosterLayer) = edit { it.updateLayer(id, change) }
    fun add(layer: PosterLayer) = edit { it.copy(layers = it.layers + layer, selectedId = layer.id) }

    fun remove(layer: PosterLayer) = edit { doc ->
        val list = if (layer.slot == null) doc.layers.filterNot { it.id == layer.id }
        else doc.updateLayer(layer.id) { it.copy(asset = null, cutout = null) }.layers
        doc.copy(layers = list, selectedId = if (layer.slot == null) null else layer.id)
    }

    fun reorder(id: String, delta: Int) = edit { doc ->
        val layers = doc.layers.toMutableList()
        val from = layers.indexOfFirst { it.id == id }
        if (from < 0) doc else {
            val to = (from + delta).coerceIn(layers.indices)
            layers.add(to, layers.removeAt(from))
            doc.copy(layers = layers)
        }
    }

    fun startGesture() { gestureStart = _document.value }

    fun transform(id: String, dx: Float, dy: Float, zoom: Float, degrees: Float) {
        val doc = _document.value ?: return
        _document.value = doc.updateLayer(id) {
            if (it.locked) it else it.copy(x = (it.x + dx).coerceIn(0f, 1200f),
                y = (it.y + dy).coerceIn(0f, 900f), scale = (it.scale * zoom).coerceIn(.15f, 6f),
                rotation = (it.rotation + degrees) % 360f)
        }
    }

    fun endGesture() {
        val start = gestureStart ?: return
        gestureStart = null
        val doc = _document.value ?: return
        if (start.id != doc.id || start.layers == doc.layers) return
        remember(start)
        save(doc)
    }

    fun import(uri: Uri, target: String, documentId: String) = viewModelScope.launch {
        try {
            val asset = store.import(uri)
            val valid = withContext(Dispatchers.IO) {
                if (target.startsWith("font:")) runCatching { Typeface.createFromFile(store.asset(asset)) }.isSuccess
                else renderer.load(asset, 256)?.let { it.recycle(); true } ?: false
            }
            if (!valid || _document.value?.id != documentId) {
                withContext(Dispatchers.IO) { store.asset(asset).delete() }
                if (!valid) notice.value = "Choose a supported image or font file."
                return@launch
            }
            when {
                target == "global" -> edit { it.copy(background = Backdrop(image = asset)) }
                target == "new" -> add(PosterLayer(kind = LayerKind.PHOTO, name = "Imported image", asset = asset))
                target.startsWith("background:") -> layer(target.substringAfter(':')) { it.copy(backdrop = Backdrop(image = asset)) }
                target.startsWith("font:") -> layer(target.substringAfter(':')) { it.copy(fontAsset = asset) }
                else -> edit { it.updateLayer(target) { photo -> photo.copy(asset = asset, cutout = null) }.copy(selectedId = target) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            notice.value = e.message ?: "Import failed."
        }
    }

    fun segment(layer: PosterLayer) {
        val doc = _document.value ?: return
        val key = layer.asset ?: return
        if (busy.value) return
        cancelCutout()
        busy.value = true
        cutoutJob = viewModelScope.launch {
            try {
                val output = BackgroundCutout(store, renderer).create(key)
                cutout.value = CutoutPreview(doc.id, layer.id, key, output)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice.value = e.message ?: "Background removal failed."
            } catch (_: OutOfMemoryError) {
                notice.value = "This device does not have enough memory to remove the background."
            } finally {
                busy.value = false
            }
        }
    }

    fun applyCutout() {
        val preview = cutout.value ?: return
        if (_document.value?.id == preview.documentId &&
            _document.value?.layers?.find { it.id == preview.layerId }?.asset == preview.original) {
            layer(preview.layerId) { it.copy(cutout = preview.image) }
            cutout.value = null
        } else cancelCutout()
    }

    fun cancelCutout() {
        cutoutJob?.cancel()
        cutout.value?.let { preview ->
            viewModelScope.launch(Dispatchers.IO) { store.asset(preview.image).delete() }
        }
        cutout.value = null
    }
}
