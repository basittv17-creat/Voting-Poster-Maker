# Legacy feature parity

| Existing feature | Native component | Implementation status | Test result |
| --- | --- | --- | --- |
| Launcher and app identity | Native MainActivity and theme | Initial native entry point and preserved application ID | Install and signed upgrade pending |
| Independent A/B photo import, replacement, pan and zoom | Photo Picker, EditorViewModel and PosterCanvas | Initial native implementation | Layer state isolation and hit-test unit checks passed. Direct A/B selection, drag isolation and gesture Undo/Redo verified on API 35. Picker replacement and multi-touch remain open. |
| Global solid backgrounds | Background toolbar and PosterRenderer | All 15 legacy swatches plus new gradients | Visual parity pending |
| Green & Gold, Tagda Muqabla #1, white divider | Frame assets and PosterRenderer | Asset extraction and frame selection | Visual parity pending |
| Text, typeface, stroke, background and resize | Text layer and StaticLayout | Multiline text, Android shaping, font import, basic styling and transforms | Urdu serialization and visual check of the mixed Urdu/English PNG passed. Imported fonts and preview comparison remain open. |
| Eight 3D text backgrounds | Text presets | Eight legacy presets carry gradient, text fill, outline and glow colors, with Android font equivalents | Pixel comparison and typography-device matrix pending |
| Sticker and emoji palettes | Glyph layers | Initial native catalogs | Device verification pending |
| Ten ratios plus original 1280 × 1100 | Canvas toolbar and Canvas size export | All eleven named legacy dimensions available | Visual comparison and native-size export UI verification pending |
| PNG gallery export and file sharing | Native PosterExport | Native PNG/JPEG renderer and share routing implemented | MediaStore save and readable share attachments verified on API 35. Share-sheet recipient and API 24–28 picker remain open. |
| WhatsApp and Facebook fallback | Android share sheet | Installed apps can receive the image attachment | Device verification pending |
| Dormant A/B badge functions and third PNG | Archived sources and extracted PNG | Preserved for baseline investigation | Released behavior unverified |
