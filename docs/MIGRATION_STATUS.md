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
3. Finish editor gaps including manual mask refinement, higher-resolution mask storage, background artwork categories, dedicated template management, additional photo effects (blur and sharpness), text letter spacing and curved text, grouping, copy/paste, multi-selection, smart alignment and richer VS artwork. Add targeted instrumentation and complete device QA for Urdu shaping, save/reopen, export and small screens.
4. Validate signing certificate lineage and the upgrade path. Keep the migration in draft until the acceptance criteria pass.
