# Legacy feature parity

| Existing feature | Native component | Implementation status | Test result |
| --- | --- | --- | --- |
| Launcher and app identity | Native MainActivity and theme | Initial native entry point and preserved application ID | Install and signed upgrade pending |
| Independent A/B photo import, replacement, pan and zoom | Photo Picker, EditorViewModel and PosterCanvas | Initial native implementation | Device verification pending |
| Global solid backgrounds | Background toolbar and PosterRenderer | Initial palette | Full swatch parity pending |
| Green & Gold, Tagda Muqabla #1, white divider | Frame assets and PosterRenderer | Asset extraction and frame selection | Visual parity pending |
| Text, typeface, stroke, background and resize | Text layer and StaticLayout | Basic text and transform controls | Urdu export and typography parity pending |
| Eight 3D text backgrounds | Text presets | Pending | Unexecuted |
| Sticker and emoji palettes | Glyph layers | Initial native catalogs | Device verification pending |
| Ten ratio presets | Canvas toolbar | Initial native presets | Device verification pending |
| PNG gallery export and file sharing | Native PosterExport | Initial native implementation | Gallery/share verification pending |
| WhatsApp and Facebook fallback | Android share sheet | Installed apps can receive the image attachment | Device verification pending |
| Dormant A/B badge functions and third PNG | Archived sources and extracted PNG | Preserved for baseline investigation | Released behavior unverified |
