# Migration status

The native editor is a development checkpoint in [draft PR #1](https://github.com/basittv17-creat/Voting-Poster-Maker/pull/1). The authorized baseline is `d9852d82baf5a2f415aacec26137b5c2fa6df90a`. The existing Capacitor app and release workflow still build from `main`. Native code is in `native-android/`.

| Phase | Current checkpoint |
| --- | --- |
| Audit and foundation | Source inventory and parity matrix recorded. Kotlin, Material 3 Compose, Room, DataStore and native rendering are in place. |
| Home, templates and editors | Both modes, bundled voting frames, editable slots, layer-scoped photo adjustments and basic tool panels implemented. |
| Background removal | Bundled model present in the APK. Per-layer preview, apply, cancel, restore and undo have implementation coverage. The engine workflow passed in airplane mode on an API 35 emulator for both modes. Interactive Photo Picker and toolbar QA remain open. |
| Background library and design tools | Solids, named gradients and imported backgrounds scope to the canvas or a photo. Initial VS styles, shapes, frames, stickers, text, font import, layer ordering, viewport controls and history are implemented. Broader artwork, styling and editor conveniences remain open. |
| Projects and export | Room stores editable drafts. Native PNG/JPEG renderer supports 1280, 1920 and 3840px long-edge presets, with gallery, document-picker and share routing. Device QA remains open. |
| Release | Development APK and an API 35 home-screen capture available. Production signing, signed upgrade and full feature parity verification are open. |

## Continue from this branch

1. Check out the draft PR head on a KVM-backed Android emulator or physical device. Use the successful build and instrumentation artifacts linked in `TEST_RESULTS.md` as the baseline.
2. Execute the remaining manual `TEST_PLAN.md` scenarios, starting with interactive Photo Picker and toolbar walkthroughs in airplane mode for both modes. Record per-step results and screenshots. Compare the original implementation on the same device, including crop gestures, framing, artwork and export.
3. Finish editor gaps including manual mask refinement, higher-resolution mask storage, background artwork categories, dedicated template management, additional photo effects (blur and sharpness), text letter spacing and curved text, grouping, multi-selection, smart alignment and richer VS artwork. Add targeted instrumentation and complete device QA for Urdu shaping, save/reopen, export and small screens.
4. Validate signing certificate lineage and the upgrade path. Keep the migration in draft until the acceptance criteria pass.

## Continuation from the Drive handoff

The five supplied mockups guided the dark toolbar and status bar treatment, panel dismissal and compact two-mode navigation checks. This change leaves the existing artwork catalogs and Capacitor release workflow in place.

The native CI build now assembles both APKs in one job. The emulator installs those exact artifacts and executes instrumentation directly, so its test phase needs no Maven resolution. Missing emulator access fails the device job. Navigation coverage exercises both modes and Back dismissal of the docked Export panel before leaving the editor. The airplane-mode engine test also exercises PNG and JPEG through both the share content provider and MediaStore.

Release signing review confirmed matching application IDs and an increased version code (10 to 11). The native build can opt in to release signing via a private properties file. A trusted production certificate or signed baseline and an authorized signing path are required to verify lineage and run the in-place upgrade. A debug install cannot establish production upgrade compatibility.

The earlier application CI at `3914410` passed the API 35 checks for independent A/B touch and drag, both editor entry points, panel Back navigation, offline segmentation and reopening, PNG/JPEG gallery output and mixed Urdu/English PNG export. Test results and artifacts are linked from `TEST_RESULTS.md`.

Before merge, complete interactive system Photo Picker replacement, pinch/rotate and undo/redo checks, the compact layout and font scale matrix, external share recipients, API 24–28 export, full legacy artwork and template parity, remaining editor features, and production signing with a signed in-place upgrade. The existing Capacitor release continues from `main`.

## Master Command continuation

My Designs now appears near the top of the home screen. Undo and Redo are next to the photo slots. Copy layer and Paste layer use an in-app clipboard for the current session and preserve independently editable layer IDs. The offline cutout preview can show the original or cutout before Apply without writing either preview to the project.

The navigation failure did not reproduce on the original APK retry. Failure screenshots and foreground diagnostics are now captured in CI. Release gates continue to include interactive Photo Picker replacement, full gesture and layout coverage, manual mask refinement, remaining artwork and advanced tools, and a production-signed upgrade. The Capacitor release stays on `main` until these gates are met.

The [application check for ceed223](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44998128942) passed both GitHub jobs, including four API 35 instrumentation tests. Full legacy parity, the remaining editor features and the required production-signed in-place upgrade are still open.
