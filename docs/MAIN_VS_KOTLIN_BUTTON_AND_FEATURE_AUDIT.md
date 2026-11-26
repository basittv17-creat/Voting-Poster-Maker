# Voting Poster Maker — main (HTML/Capacitor) versus Kotlin/Compose button & feature audit

26 November 2026. **Authorized user decision:** keep `main` unchanged; publish an independently signed Kotlin AAB from `feature/native-kotlin-compose-rebuild` as **Internal testing** first. Do not merge until user reviews Play acceptance and all functionality tests pass.

## Sources inspected
- Shipped `main` exact HTML Git blob `35467d271251335b1d7471eb1fa6a42b76848410` (`www/index.html` ~4.34 MB), including actual generated toolbar HTML, click handlers, data, fonts, frame lists, image inputs and export/share handlers. `main` production app remains versionCode 11, while historical versionCode 9 APK/AAB also used for runtime comparison.
- Native Kotlin source: `MainActivity.kt`, `EditorTools.kt`, `PosterCanvas.kt`, `PosterDocument.kt`, `LegacyPresets.kt`, `EditorViewModel.kt`, `PosterRenderer.kt`, `PosterExport.kt` and instrumented tests from current draft PR #1.
- API 29 and 35 **separate clean-install, actual emulator launch** of historical version 9 APK and pinned Kotlin version 12 signed candidate succeeded in [run 45829979390](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45829979390). The historical original Tagda Muqabla frame is visible on the API 35 screenshot. The new APK's home includes Voting Poster / Single Poster Design / My Designs.
- **Not yet proven:** every function tapped and edited on both builds; export pixel comparisons; all photo picker/gesture/background mask tests in the most recent CI run; production Play-distributed in-place update.

## Every user-facing old toolbar button and Kotlin counterpart

| Existing main/HTML control | Kotlin + Compose counterpart | Evidence-based status |
| --- | --- | --- |
| **A: Add A Photo** | Voting A slot / Photo Picker, direct selected-photo toolbar | Implemented source; last UI automation had intermittent A button selection failure, retest required. |
| **B: Add B Photo** | Voting B slot / Photo Picker, A and B separate stable IDs | Implemented source; retest all replacements and gestures. |
| Photo toolbar **Back** | Pinned Back in one contextual photo dock | Implemented; fixed width to avoid reflow while background mask panel opens. Device QA pending. |
| **Zoom In** and **Zoom Out** selected A/B | Layer-scoped Zoom in/out plus Crop zoom | Implemented; output parity with old fixed rectangles not proven. |
| **Reset** selected photo (old center x300/900 y410 plus size 1) | Native **Reset photo** recenters each photo in its **own original template slot**, resets scale/rotation/flips/crop without losing image | Implemented with unit test. Source geometry differs slightly for framed/unframed native templates, deliberately preserves native slot centers. |
| **Replace A/B Photo** | **Replace photo** for selected layer, with Photo Picker | Implemented source, UI test retest pending. |
| **Background** | Native Background panel, same exact 15 swatches plus original offline gradients | Source values verified identical; output QA pending. |
| **Text** / **Add Text**, **Edit Text**, **Delete Text** | Native Text/Selection + editable multiline Text layer, Delete | Implemented; text visual placement, keyboard and RTL QA pending. |
| **Text Background** (ten CSS gradients), **Remove BG** | Native **Original text background styles** Style 1 through Style 10, exact source color pairs and diagonal native renderer; Remove background plus new background library | Newly restored; model serialization and Gradle testing pending. |
| **3D Backgrounds** (8 Royal Gold, Neon Purple, Fire Red, Ice Blue, Emerald, Carbon Gold, Royal Red, Cyber) | Native **3D Backgrounds** direct main toolbar entry + selection panel, same eight retained presets | Implemented source, user-facing direct entry now restored; fonts are Android equivalents, exact visual parity pending. |
| **Text Color** (15 swatches) | Native dedicated original 15 text swatches, separate from the 15 poster background swatches | Now restored by exact main-source color values, device/apply test pending. |
| **Font Change** | Native Sans / Serif / Mono and user-imported font | Implemented but original HTML fonts are not one-to-one embedded equivalents. More font/RTL visual QA needed. |
| **Style Change** (outline palette, Thin/Thick/Off) | Native outline color and Stroke amount slider, shadow color and shadow size | Equivalent core effects; exact style samples and pixels pending. |
| **Zoom Text + / −** | Native Font size and Resize sliders, on-canvas transforms | Equivalent functionality by different control; automated UI verification needed. |
| **Stickers** and **Emoji** | Native Elements catalog + independently editable glyph layers | 15 old sticker entries and 14 old emoji entries, including 🍼💧; source catalog parity verified, glyph font image output pending. |
| **Frame** > Green & Gold, Tagda Muqabla #1 | Native exact bundled PNG frame templates **and in-editor Frame switcher** (No frame, Green & Gold, Tagda Muqabla #1) | Now implemented as non-destructive frame switching retaining A/B photo assets/cutouts/text/stickers, covered by new unit test; real visual/export QA pending. |
| **Canvas** (11 ratios/sizes including 1280 × 1100) | Native Canvas preset panel (11) and canvas-size export | Values match source; export/view aspect QA pending. |
| **Save Poster** to Gallery | Native Export > Save (PNG), MediaStore, plus JPEG, native-sized/1280/1920/3840 output | Implemented; tests cover API 35 MediaStore but older API and final recipient gallery QA pending. |
| **WhatsApp** | Native Export > WhatsApp, actual image URI with alternate WhatsApp Business then Android chooser fallback | Newly restored in Kotlin source; actual recipient/device test pending. |
| **Facebook** | Native Export > Facebook, actual image URI with Android chooser fallback | Newly restored in Kotlin source; recipient/device QA pending. |
| **Share Others** | Native Export > Share others via Android chooser | Implemented; Android image permission URI grant; device QA pending. |
| Canvas or photo gestures | Native direct hit testing, drag, pinch, rotate, undo/redo | Implemented, currently 3/9 instrumented UI tests failed on prior PR run; fix/retest before approval. |

## Other visible differences and actual additional Kotlin functions
Kotlin adds **two modes** (A/B and Single Poster), **My Designs** (Room autosave), independent layer selection and z-order, Undo/Redo, **offline on-device Remove Background**, manual mask erase/restore/refine/feather/compare/apply/cancel, per-subject separate background, gradients, PNG/JPEG, high-res export, importable fonts, layers lock/hide/duplicate, filters, and native share attachments. These are real code paths, not a demand from Google Play.

**Requested native extras in this session**:
1. **Fast Flip Horizontal / Flip Vertical and nondestructive Reset Photo** in selected-photo dock, all undoable.
2. **Three original offline templates**: Teal Community Vote, Copper vs Navy, Emerald Event.
3. In addition to extras, restore old buttons/options: ten original text-background gradients, fifteen original text-color swatches, direct eight-style 3D Backgrounds panel, two in-editor frame choices, WhatsApp/Facebook/Other buttons and original per-slot photo re-centering. Keep all old native features.

## Important outstanding parity gaps
- The original frame switcher and separate 15 text-color palette have now been **implemented in the Kotlin source**. They still need device-level selection and PNG/JPEG render comparison before parity can be considered verified.
- Old installed app has poster export preview modal and top-level immediate A/B editor; native has a two-mode home and an Export panel. Functionality is related but presentation is intentionally redesigned. Verify the owner accepts the new flow.
- Photo/selected text hit area, Urdu font rendering, 10 CSS-gradient 135-degree pixel parity, 3D fonts and actual export comparisons are not fully proven.
- The old A/B **badge** scripts are deliberately dormant (`showABTools` immediately returns); not counted as shipped UI without actual baseline evidence.
- Before submitting a Play update, check version code is greater than the **actual highest uploaded Play version**, app signing/upload certificate pair, latest exact-head signed AAB and full device QA; use Play Console Internal testing first. **Same UI or identical buttons are not requirements imposed by Play Console**. Do not misrepresent code functionality or approval status.

## Protection and release status
Never change `basittv17-creat/Voting-Pro`. Never merge draft PR #1 or retire `main` Capacitor while Play Internal testing and project release gates are pending. Build and upload candidate **only from a verified passing exact commit**, and report honest blockers. See `docs/PLAY_CONSOLE_KOTLIN_INTERNAL_TEST.md` and `docs/FEATURE_PARITY_MATRIX.md`.
