# Native Android verification

Application source reviewed at commit `2671f6f9b90ef17150ce2b62c970b8ec521151b0`. The GitHub tree and the locally built source tree match. Local tools were Android SDK 36, build tools 36.0.0, Gradle 8.13 and Temurin JDK 17. The screenshot workflow correction at `215f31efa063444b355bfac233dc0d20a6bd6d29` changed no application source.

| Check | Result |
| --- | --- |
| `:app:assembleDebug` | Passed. Produced a debug-signed APK for `com.awisogera.votingpostermaker`, version 11.0, minimum API 24 and target API 36. |
| `:app:testDebugUnitTest` | Passed 3 of 3. A/B layer state isolation, rotated/ordered hit testing, Urdu serialization and selection round trip. |
| `:app:lintDebug` | Passed with 0 errors and 14 warnings. |
| `:app:assembleDebugAndroidTest` | Passed. Compiled and packaged the Android airplane-mode smoke test. |
| APK validation | ZIP integrity and APK Signature Scheme v2 verified. APK holds the segmentation graph, a bundled TFLite model and both reusable legacy frames. The manifest does not request Internet access. |
| Local emulator | Launch aborted before boot. The execution machine lacks KVM and the installed x86_64 emulator requires it. |
| Legacy source baseline | JavaScript syntax passed. Both generated frame files matched their legacy PNG payloads byte for byte. Legacy app and release workflow had no changes. |
| GitHub debug build and unit tests | Passed. [Download the debug APK ZIP from the successful rerun](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44990746960/artifacts/14742235648). |
| Airplane-mode engine smoke test | Passed on the GitHub API 35 emulator (1 test, 0 failures). Covers import, segmentation, replacements, editable Room round trip and PNG export in both modes. [Instrumentation XML and screenshot artifact](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44990746960/artifacts/14741437934). |
| Android home screen | Verified on a 320×640 API 35 emulator. Both mode cards are visible and UI Automator confirmed the second card. The first run missed the screenshot because Gradle removed the test installation. Reinstalling the APK resolved capture on the successful rerun. Dark system status icons on the dark app remain a visual polish item. |
| Manual UI, lifecycle, typography, 4K memory and signing | Pending. These require device interaction, visual inspection and access to the signed upgrade build. |

Debug APK SHA-256: `819fe27b4ca39e518c01c3849182cbdf56a8897721b8b88375ca5abd4619dfa6`.

The emulator smoke test imports a private local portrait through the storage code, runs segmentation in both modes, checks transparent and foreground pixels, applies independent backdrops, reopens the saved record from Room and decodes a rendered PNG. It complements the manual Photo Picker, gestures, Urdu rendering, gallery saving and share-sheet scenarios in the test plan.

## Migration continuation

- [Original device attempt](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44991199900/job/137112418991) failed during Maven Central dependency resolution with HTTP 429. [The retried run](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44991199900) passed both jobs.
- [Prebuilt APK workflow](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44995539396) passed on API 35. It exercised entering both modes, dismissing the Export panel with Android Back, and offline segmentation, save/reopen, PNG/JPEG output, MIME type and readable data through FileProvider and MediaStore. The system share-sheet recipient path was outside this check.
- [Selection and Urdu evidence](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44995685625/artifacts/14743468006) contains four passing device tests, including direct A/B photo taps through the real canvas and a 1200×900 transparent PNG with multiline Urdu and mixed English. The exported sample was visually checked for joined glyphs and mixed-direction order. Imported Urdu fonts and the wider typography matrix are pending.
- Shell fixture checks passed for the CI instrumentation success gate and APK comparison script. The APK script accepted the matching identity, signer and higher version fixture, and rejected an invalid signature, a different signer, an incorrect app ID and zero version increase. Production certificate verification and the signed in-place upgrade remain pending.
- [Latest application changes](https://github.com/basittv17-creat/Voting-Poster-Maker/commit/3914410b5e9ba2ff330162d5f71d81cd83d6dbb4) passed the [full native CI run](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44996004843). Gradle completed unit tests, both APK builds, and `:app:lintDebug`. The API 35 instrumentation runner reported `OK (4 tests)`, including touching and dragging each seeded A/B image, checking the other layer stayed unchanged in Room, and retaining the Urdu export and both file formats. [Device evidence](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44996004843/artifacts/14743353850) · [Debug APK](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44996004843/artifacts/14744300295).

## Navigation failure and retry

[Run 44996490378, first device attempt](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44996490378/job/137129524590) built both APKs but failed on API 35 with missing My Designs and Voting Poster accessibility nodes. The failure artifact contained only the instrumentation transcript, so it could not distinguish an app surface from a system overlay or clipping. The [same commit, same APK device retry](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44996490378/job/137133123046) passed. The failure is intermittent and independent of the older Maven HTTP 429.

The follow-up moves My Designs ahead of the two scrolled mode cards, requires the home heading before tapping navigation controls, and scrolls visible containers when a target is outside the viewport. CI now wakes and dismisses the emulator keyguard before tests and captures the foreground screenshot, visible nodes and system diagnostics on a failure. The Android suite additionally verifies Undo and Redo after dragging each photo. The layer-duplication unit test checks the pasted photo retains its edits and never inherits the original A/B slot.

[Run 44998128942](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44998128942) verified application commit `ceed223040248b9fb1a936d523678c7cc0dfde6f`. Unit tests, app and instrumentation builds, and Android lint passed. On the API 35 airplane-mode emulator, the instrumentation runner reported `OK (4 tests)`. These cases covered both home mode routes, Back from the export panel, A/B selection and drag isolation, gesture Undo and Redo, offline segmentation and project reopening, PNG/JPEG gallery and share-provider output, and an exported Urdu sample. The before/after cutout controls and the copy/paste button flow still need direct UI coverage. [Device evidence](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44998128942/artifacts/14744407608) · [Debug APK](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44998128942/artifacts/14744966892).

The subsequent documentation-head [run 44998601344](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/44998601344) passed verify but failed the device suite at “Could not tap Blue vs Purple.” The label was present. Android rejected the accessibility click action. The navigation test now taps the visible bounds through Android input and still waits for each target screen and persisted state.

## Mask and legacy-settings continuation

- [Run 45004198415](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45004198415) passed build, unit tests and lint. Five of six device tests passed, including the mask pixel and 3840 × 2880 export checks. The navigation screenshot showed the Single template gallery had scrolled before the tap reached its target. The navigation test now waits for the gallery item before scrolling and touching.
- [Run 45004420012](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45004420012) caught a redundant `@RequiresApi` annotation on the new Photo Picker test. The test now uses the Android test runner's `@SdkSuppress` alone. The live PR check determines the status of the revised tests.


- [Run 45004817536](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45004817536) passed both jobs at `5c362b2404731b2d3eddf3a4964a70477d75a8fd`. The API 35 runner reported eight passing tests. System Photo Picker import and replacement passed for A, B and Single, with pixel and layer-isolation assertions. The mask UI test compared, refined, applied and undid a cutout. The engine tests verified brush alpha and color, 3840 × 2880 output, offline segmentation and PNG/JPEG. Source comparison matched eleven legacy dimensions and eight legacy palettes. The follow-up adds two-finger gesture assertions and captures the mask brush dialog in the device artifact.
