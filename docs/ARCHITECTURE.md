# Native architecture

The migration project lives in `native-android/` with original Android and web sources beside it. Its Android application ID is `com.awisogera.votingpostermaker`.

- Compose hosts mode selection, template galleries, editor controls, settings and saved designs. The editor docks its active panel between the canvas and the lower toolbar.
- `PosterDocument` stores the mode, logical canvas state and ordered layer list with stable IDs. `EditorViewModel` owns StateFlow state, history, photo-scoped changes, serialized autosave writes and background-removal previews.
- `PosterRenderer` draws into Android Canvas for both Compose preview and bitmap export. It lays out multiline text with StaticLayout and first-strong bidi direction. The 1200×900 logical coordinate system maps into each aspect-ratio preset. A preset can stretch a design, as in the legacy editor.
- `ProjectStore` uses Room for editable designs and private internal files for source images, imported fonts and transparent cutouts. DataStore persists the resume preference. Save history is in memory and starts fresh when reopening a project.
- Export uses FileProvider for sharing and MediaStore on API 29+. API 24–28 use Android's document picker.
- The native build packages exact copies of both selectable legacy frame PNGs from the archived HTML. Their artwork remains inside the repository's original asset source.
