package com.awisogera.votingpostermaker

import android.content.ClipData
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

private val night = Color(0xff080d1b)
private val panelColor = Color(0xff151f38)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xff64e5fb), secondary = Color(0xffc8afff),
                tertiary = Color(0xffffd98a), background = night, surface = panelColor,
            )) { PosterApp(viewModel()) }
        }
    }
}

@Composable
private fun PosterApp(vm: EditorViewModel) {
    val document by vm.document.collectAsStateWithLifecycle()
    val savedProjects by vm.projects.collectAsStateWithLifecycle(initialValue = null)
    val projects = savedProjects.orEmpty()
    val notice by vm.notice.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var mode by rememberSaveable { mutableStateOf(PosterMode.VOTING) }
    val messages = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context.applicationContext) { EditorPreferences(context.applicationContext) }
    val resume by preferences.resumeDesign.collectAsStateWithLifecycle(initialValue = null)
    var startupHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(resume, savedProjects, screen) {
        if (!startupHandled && resume != null) {
            if (resume == true && savedProjects == null && screen == "home") return@LaunchedEffect
            startupHandled = true
            if (resume == true && screen == "home") {
                val latest = projects.firstOrNull()
                if (latest != null && vm.open(latest)) {
                    mode = vm.document.value?.mode ?: mode
                    screen = "editor"
                }
            }
        }
    }
    LaunchedEffect(notice) {
        notice?.let {
            messages.showSnackbar(it)
            if (vm.notice.value == it) vm.notice.value = null
        }
    }
    // A process restart leaves drafts in My Designs.
    val current = document
    val page = if (screen == "editor" && current == null) "projects" else screen
    BackHandler(page != "home") {
        vm.cancelCutout()
        screen = if (page == "editor") "gallery" else "home"
    }
    Scaffold(containerColor = night, snackbarHost = { SnackbarHost(messages) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (page != "editor") {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (page != "home") TextButton(onClick = { screen = "home" }) { Text("Back") }
                    Text("Voting Poster Maker", style = MaterialTheme.typography.titleLarge)
                }
            }
            when (page) {
                "home" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Create a poster", style = MaterialTheme.typography.headlineLarge)
                    OutlinedButton(onClick = { screen = "projects" }, modifier = Modifier.fillMaxWidth()) { Text("My Designs") }
                    ModeCard(PosterMode.VOTING, "Design a side-by-side poster with two photos.", Color(0xff183876)) {
                        mode = PosterMode.VOTING; screen = "gallery"
                    }
                    ModeCard(PosterMode.SINGLE, "Design a campaign, event or announcement around one photo.", Color(0xff48247a)) {
                        mode = PosterMode.SINGLE; screen = "gallery"
                    }
                    Row {
                        TextButton(onClick = { screen = "settings" }) { Text("Settings") }
                        TextButton(onClick = { screen = "help" }) { Text("Help") }
                    }
                }
                "gallery" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(mode.title, style = MaterialTheme.typography.headlineMedium)
                    templates(mode).forEach { template ->
                        val preview = remember(template, mode) { template.create(mode) }
                        Card(onClick = { vm.open(template.create(mode)); screen = "editor" }) {
                            PosterCanvas(preview, vm.renderer, Modifier.fillMaxWidth().height(150.dp))
                            Text(template.name, Modifier.padding(14.dp), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                "projects" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("My Designs", style = MaterialTheme.typography.headlineMedium)
                    if (projects.isEmpty()) Text("Your editable designs will appear here.")
                    projects.forEach { item ->
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                            if (vm.open(item)) {
                                mode = vm.document.value?.mode ?: mode
                                screen = "editor"
                            }
                        }) { Text(item.title) }
                    }
                }
                "settings" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Resume latest design on launch", Modifier.weight(1f))
                    Switch(checked = resume == true, onCheckedChange = { enabled ->
                        scope.launch {
                            try { preferences.setResumeDesign(enabled) }
                            catch (e: CancellationException) { throw e }
                            catch (_: Exception) { vm.notice.value = "Could not save settings." }
                        }
                    })
                }
                "help" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Editing a poster", style = MaterialTheme.typography.headlineMedium)
                    Text("Choose a template. Tap a photo slot to import a picture. Tap any visible unlocked object to edit it. Drag to move it, and use two fingers to resize or rotate it.")
                    Text("Changes are saved automatically in My Designs. Use Export to save or share an image.")
                }
                "editor" -> if (current != null) EditorScreen(current, vm) {
                    vm.cancelCutout()
                    screen = "home"
                }
            }
        }
    }
}

@Composable
private fun ModeCard(mode: PosterMode, subtitle: String, color: Color, open: () -> Unit) {
    Card(onClick = open, shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(color, panelColor))).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(mode.title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, color = Color(0xffdbe8ff))
            Text("Choose template", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ColumnScope.EditorScreen(document: PosterDocument, vm: EditorViewModel, close: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard by vm.clipboard.collectAsStateWithLifecycle()
    val processing by vm.busy.collectAsStateWithLifecycle()
    val preview by vm.cutout.collectAsStateWithLifecycle()
    var panel by rememberSaveable(document.id) { mutableStateOf("") }
    var pickTarget by rememberSaveable { mutableStateOf("") }
    var pickDocument by rememberSaveable { mutableStateOf("") }
    var pendingExport by rememberSaveable { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var format by rememberSaveable { mutableStateOf(ExportFormat.PNG) }
    var edge by rememberSaveable { mutableIntStateOf(1920) }
    var viewportZoom by rememberSaveable(document.id) { mutableFloatStateOf(1f) }
    var viewportPan by remember(document.id) { mutableStateOf(Offset.Zero) }
    var moveCanvas by rememberSaveable(document.id) { mutableStateOf(false) }
    var showGrid by rememberSaveable(document.id) { mutableStateOf(false) }
    val selected = document.selected
    val photo = selected?.takeIf { it.kind == LayerKind.PHOTO && it.visible && !it.locked }
    val activePreview = preview?.takeIf { it.documentId == document.id && it.layerId == photo?.id }
    var refine by remember(document.id, photo?.id) { mutableStateOf(false) }
    var showOriginal by remember(activePreview?.image) { mutableStateOf(false) }
    var checkerboard by remember(activePreview?.image) { mutableStateOf(true) }
    fun dismissPanel() {
        if (panel == "Remove BG") vm.cancelCutout()
        refine = false
        panel = ""
    }
    fun editorBack() {
        when {
            refine -> refine = false
            panel.isNotEmpty() -> dismissPanel()
            selected != null -> vm.select(null)
        }
    }
    BackHandler(panel.isNotEmpty() || selected != null, onBack = ::editorBack)
    fun openPanel(name: String) {
        if (panel == "Remove BG" && name != panel) vm.cancelCutout()
        refine = false
        panel = name
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        if (it != null) vm.import(it, pickTarget, pickDocument)
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) vm.import(it, pickTarget, pickDocument)
    }
    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data.takeIf { result.resultCode == android.app.Activity.RESULT_OK }
        val file = pendingExport?.let(::File)
        pendingExport = null
        if (uri != null && file != null) scope.launch {
            try {
                vm.exporter.copyTo(file, uri)
                vm.notice.value = "Poster saved."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { vm.notice.value = "Could not save the poster." }
        }
    }
    fun pick(target: String) {
        pickTarget = target
        pickDocument = document.id
        if (target.startsWith("font:")) fontPicker.launch(arrayOf("*/*"))
        else picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun export(share: Boolean, preferredPackage: String? = null) {
        if (exporting) return
        exporting = true
        scope.launch {
            try {
                val file = vm.exporter.render(document, if (edge == 0) maxOf(document.width, document.height) else edge, format)
                when {
                    share -> {
                        val uri = vm.exporter.shareUri(file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = format.mime
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        if (preferredPackage == null) {
                            context.startActivity(Intent.createChooser(intent, "Share poster"))
                        } else {
                            // Match the legacy WhatsApp/Facebook actions, attaching the real PNG/JPEG.
                            // Not every phone has those apps; let the user choose another recipient.
                            try {
                                context.startActivity(Intent(intent).setPackage(preferredPackage))
                            } catch (_: ActivityNotFoundException) {
                                try {
                                    if (preferredPackage == "com.whatsapp") {
                                        context.startActivity(Intent(intent).setPackage("com.whatsapp.w4b"))
                                    } else {
                                        context.startActivity(Intent.createChooser(intent, "Share poster"))
                                    }
                                } catch (_: ActivityNotFoundException) {
                                    context.startActivity(Intent.createChooser(intent, "Share poster"))
                                }
                            }
                        }
                    }
                    Build.VERSION.SDK_INT >= 29 -> {
                        vm.exporter.saveToGallery(file, format)
                        vm.notice.value = "Poster saved to gallery."
                    }
                    else -> {
                        pendingExport = file.path
                        savePicker.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = format.mime
                            putExtra(Intent.EXTRA_TITLE, file.name)
                        })
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: OutOfMemoryError) {
                vm.notice.value = "Try exporting at a smaller resolution."
            } catch (e: Exception) {
                vm.notice.value = e.message ?: "Could not export the poster."
            } finally {
                exporting = false
            }
        }
    }

    val displayed = if (activePreview?.temporary != true) document else document.updateLayer(activePreview.layerId) {
        if (it.asset == activePreview.original) it.copy(cutout = if (showOriginal) null else activePreview.image) else it
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = close) { Text("Home") }
        Text(photo?.name ?: document.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = { openPanel("Export") }) { Text("Export") }
    }
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val panelHeight = (maxHeight * .55f).coerceAtMost(300.dp)
        Column(Modifier.fillMaxSize()) {
            PosterCanvas(displayed, vm.renderer, Modifier.weight(1f).fillMaxWidth(),
                vm = vm, inputEnabled = panel != "Remove BG",
                checkerboardLayer = activePreview?.takeIf { it.temporary && !showOriginal && checkerboard }?.layerId,
                zoom = viewportZoom, pan = viewportPan, moveCanvas = moveCanvas, showGrid = showGrid,
                onViewport = { zoom, pan -> viewportZoom = zoom; viewportPan = pan },
            ) { layer, tapped ->
                val contextualPhoto = layer?.kind == LayerKind.PHOTO
                if (layer != null) {
                    panel = if (contextualPhoto) "" else "Selection"
                    if (tapped && contextualPhoto && layer.asset == null) pick(layer.id)
                } else if (tapped) panel = ""
            }
            if (panel.isNotEmpty()) key(panel, selected?.id, refine) {
                Surface(color = panelColor, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().heightIn(max = panelHeight)
                        .semantics { contentDescription = "Editor panel" }.verticalScroll(rememberScrollState()).padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (panel == "Selection") selected?.name ?: "Selection" else panel, Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = ::dismissPanel) { Text("Close") }
                        }
                        when (panel) {
                            "Remove BG" -> if (photo != null) {
                                when {
                                    processing -> {
                                        LinearProgressIndicator(Modifier.fillMaxWidth())
                                        Text("Removing background on this device…")
                                        Tool("Cancel", ::dismissPanel)
                                    }
                                    refine && activePreview != null -> MaskRefinementPanel(activePreview, vm) { refine = false }
                                    activePreview != null && activePreview.temporary -> {
                                        Text("Preview background removal")
                                        ToolRow {
                                            Tool("Apply") { vm.applyCutout(); panel = "" }
                                            Tool("Cancel", ::dismissPanel)
                                            Tool("Refine edges") { refine = true }
                                        }
                                        ToolRow {
                                        Tool(if (showOriginal) "Show cutout" else "Show original") { showOriginal = !showOriginal }
                                        if (!showOriginal) Tool(if (checkerboard) "On poster" else "Checkerboard") { checkerboard = !checkerboard }
                                    }
                                    }
                                    else -> {
                                        ToolRow {
                                            Tool("Auto remove") { vm.segment(photo) }
                                            Tool("Refine edges") { vm.startManualMask(photo); refine = true }
                                        }
                                        if (photo.cutout != null) Tool("Restore original") {
                                            vm.layer(photo.id) { it.copy(cutout = null) }
                                            dismissPanel()
                                        }
                                    }
                                }
                            }
                            "Crop" -> if (photo != null) CropTools(photo, vm)
                            "Adjust" -> if (photo != null) PhotoAdjustments(photo, vm)
                            "Selection" -> if (selected != null) LayerTools(selected, vm, ::pick) { panel = it }
                            "Layers" -> LayersPanel(document, vm) { panel = "Selection" }
                            "Background" -> {
                                BackgroundTools { bg -> vm.edit { it.copy(background = bg) } }
                                Tool("Import background") { pick("global") }
                            }
                            "Photo Background" -> {
                                val photo = selected?.takeIf { it.kind == LayerKind.PHOTO && !it.locked }
                                if (photo == null) {
                                    Text("Select an unlocked photo to change its background.")
                                } else {
                                    BackgroundTools { bg -> vm.layer(photo.id) { it.copy(backdrop = bg) } }
                                    Tool("Import background") { pick("background:${photo.id}") }
                                }
                            }
                            "Elements" -> ElementTools(vm)
                            "VS Styles" -> VsTools(document, vm)
                            "Canvas" -> {
                                ToolRow {
                                    Tool(if (moveCanvas) "Edit layers" else "Pan / pinch canvas") { moveCanvas = !moveCanvas }
                                    Tool("Zoom in") { viewportZoom = (viewportZoom + .25f).coerceAtMost(4f) }
                                    Tool("Zoom out") {
                                        viewportZoom = (viewportZoom - .25f).coerceAtLeast(1f)
                                        if (viewportZoom == 1f) viewportPan = Offset.Zero
                                    }
                                    Tool("Reset view") { viewportZoom = 1f; viewportPan = Offset.Zero; moveCanvas = false }
                                    Tool(if (showGrid) "Hide grid" else "Show grid") { showGrid = !showGrid }
                                }
                                CanvasTools(document, vm)
                            }
                            "Export" -> {
                                ToolRow {
                                    ExportFormat.entries.forEach { option -> FilterChip(format == option, { format = option }, { Text(option.name) }) }
                                }
                                ToolRow {
                                    FilterChip(edge == 0, { edge = 0 }, { Text("Canvas size") })
                                    listOf(1280, 1920, 3840).forEach { size ->
                                        FilterChip(edge == size, { edge = size }, { Text("${size}px") })
                                    }
                                }
                                ToolRow {
                                    Button(onClick = { export(false) }, enabled = !exporting) { Text("Save") }
                                    OutlinedButton(onClick = { export(true, "com.whatsapp") }, enabled = !exporting) { Text("WhatsApp") }
                                    OutlinedButton(onClick = { export(true, "com.facebook.katana") }, enabled = !exporting) { Text("Facebook") }
                                    OutlinedButton(onClick = { export(true) }, enabled = !exporting) { Text("Share others") }
                                }
                                if (exporting) LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = "Editor tools" },
            verticalAlignment = Alignment.CenterVertically) {
            if (photo != null) {
                // Keep the selected-photo Back affordance at a stable, accessible width
                // when the inline cutout panel opens or the tool list changes.
                TextButton(
                    onClick = ::editorBack,
                    modifier = Modifier.width(72.dp).heightIn(min = 48.dp)
                        .semantics { contentDescription = "Photo toolbar Back" },
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) { Text("Back", maxLines = 1) }
                key(photo.id) {
                    ToolRow {
                        Tool(if (photo.asset == null) "Add photo" else "Replace photo") {
                            dismissPanel()
                            pick(photo.id)
                        }
                        Tool("Zoom in") { openPanel(""); vm.layer(photo.id) { it.copy(cropZoom = (it.cropZoom * 1.15f).coerceAtMost(4f)) } }
                        Tool("Zoom out") { openPanel(""); vm.layer(photo.id) { it.copy(cropZoom = (it.cropZoom / 1.15f).coerceAtLeast(.2f)) } }
                        if (photo.asset != null) Tool("Remove BG") { openPanel("Remove BG") }
                        Tool("Backgrounds") { openPanel("Photo Background") }
                        Tool("Crop") { openPanel("Crop") }
                        Tool("Rotate") { openPanel(""); vm.layer(photo.id) { it.copy(rotation = (it.rotation + 90f) % 360f) } }
                        Tool("Flip H") { openPanel(""); vm.layer(photo.id) { it.copy(flipX = !it.flipX) } }
                        Tool("Flip V") { openPanel(""); vm.layer(photo.id) { it.copy(flipY = !it.flipY) } }
                        Tool("Reset photo") {
                            openPanel("")
                            vm.edit { it.resetPhotoToSlot(photo.id) }
                        }
                        Tool("Adjust") { openPanel("Adjust") }
                        Tool("Undo") { dismissPanel(); vm.undo() }
                        Tool("Redo") { dismissPanel(); vm.redo() }
                        Tool("Layer options") { openPanel("Selection") }
                    }
                }
            } else ToolRow {
                document.layers.filter { it.slot != null }.forEach { photo ->
                    Tool(photo.slot ?: "Photo") {
                        vm.select(photo.id)
                        panel = if (photo.visible && !photo.locked) "" else "Selection"
                        if (photo.asset == null && !photo.locked) pick(photo.id)
                    }
                }
                Tool("Undo", vm::undo)
                Tool("Redo", vm::redo)
                Tool("Background") { panel = "Background" }
                Tool("Text") {
                    vm.add(PosterLayer(kind = LayerKind.TEXT, name = "Text", width = 820f, height = 230f, text = "Your headline"))
                    panel = "Selection"
                }
                Tool("Elements") { panel = "Elements" }
                if (document.mode == PosterMode.VOTING) Tool("VS Styles") { panel = "VS Styles" }
                Tool("Layers") { panel = "Layers" }
                Tool("Canvas") { panel = "Canvas" }
                Tool("Import image") { pick("new") }
                if (clipboard != null) Tool("Paste layer") {
                    vm.pasteLayer()
                    panel = "Selection"
                }
            }
        }
    }
}

@Composable
fun ToolRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
fun Tool(label: String, action: () -> Unit) {
    OutlinedButton(onClick = action, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(label) }
}
