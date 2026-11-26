# Legacy feature inventory

Baseline: `main` at `d9852d82baf5a2f415aacec26137b5c2fa6df90a`. This is a source audit. The baseline APK has yet to be exercised on a device.

| Feature | Source | Behavior and assets | Migration and verification |
| --- | --- | --- | --- |
| Launch and branding | Android manifest, resources, MainActivity.java and Capacitor config | Launches directly into one A/B editor, with the existing launcher icon, splash resources and app ID | Preserve ID and launcher icons, launch Compose mode selection. Check a fresh install and a signed upgrade. |
| A/B photo input | HTML 151–187, 598–633 | Independent file inputs; center-cover crop into fixed A/B rectangles with Urdu placeholders | Android Photo Picker and scoped layer mutations. Verify replace, drag, pinch and rotate on each side with the other side unchanged. |
| Photo toolbar and hit testing | HTML 598–737 | Select by region, zoom in/out, recenter, replace and drag; hit testing converts touch coordinates into the fixed 1200×900 canvas | Native per-layer controls and inverse transforms. Verify selection after resizing canvas and use on a small screen. |
| Poster backgrounds | HTML 210–224 | Fifteen solid colors in a horizontally scrolling tool strip | Preserve swatches and add distinct global and per-photo background state. Verify both output regions. |
| Frames and divider | HTML 171–190, 225–245 | Visible Green & Gold and Tagda Muqabla #1 frame PNGs, fixed white divider. A third embedded frame is loaded but has no demonstrated render path. | Extract all three PNGs without recompression. Verify visible legacy frames visually against baseline. |
| Text and typography | HTML 247–394, 411–472, 611–737 | Multiline text, editing, fonts, color, size, outlines, background, drag, resize and delete | Editable text layers backed by Android StaticLayout and bidi shaping. Compare preview/export with Urdu, Hindi, Arabic and mixed-direction strings. |
| 3D text backgrounds | HTML 292–324, 193 | Eight named presets that set background, typeface, color, outline and shadow; verify actual legacy rendering of the CSS gradient value | Native gradient presets with reproducible export. Compare each preset with baseline behavior. |
| Stickers and emoji | HTML 394–408, 611–737 | Android/browser font glyphs, separate menus, move, resize and delete | Retain glyph catalogs in native layer menus. Check emoji presentation in PNG and JPEG. |
| A/B badges | HTML 183, 473–530 | Badge renderer and tool functions exist; the renderer has no demonstrated call site and showABTools returns early | Treat as dormant legacy code. Check baseline before counting as released behavior. |
| Canvas sizes | HTML 740–771 | Ten presets; fixed logical coordinates stretch into differently sized outputs | Shared coordinate space and explicit aspect presets. Verify dimensions, orientation and touch mapping. |
| Export and share | HTML 533–597, GalleryPlugin.java and file_paths.xml | PNG at canvas dimensions; Android MediaStore save with PNG validation and pending row on API 29+. Web download and share fallbacks. WhatsApp/Facebook link fallbacks may omit the image. | Off-screen native renderer, scoped MediaStore save, API 24–28 document picker and share-sheet attachment. Verify actual output across Android versions. |
| Editable projects | HTML state at 165–169; complete source scan for persistence APIs | Canvas state kept in memory. Exported PNGs remain in the user's gallery independently of the editor. | Room/autosave are new features; preserve access to existing gallery exports through the platform picker. |
| Build and signing | package.json, android builds, build-android.yml, scripts/configure-signing.py | Capacitor 6.2.1, minimum SDK 24, target 36, version code 10; signed release uses repository secrets and local keystore config | Keep legacy release intact through migration; preserve signing certificate lineage and increase version code for production cutover. |
| Permissions | AndroidManifest.xml and GalleryPlugin.java | INTERNET declared; Android export uses MediaStore; older versions need real-device verification | Native editor needs no Internet permission. Verify local pick, save and share in airplane mode. |

`www/index.html` and the Android packaged HTML are byte-identical at baseline. All legacy code and assets are retained on the migration branch.
