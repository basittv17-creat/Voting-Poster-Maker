# Historical Version 9 versus native Kotlin: evidence ledger
Date: 26 November 2026 (Asia/Karachi). **Not a production-release approval.**

## Exact sources and scope
- Historical Android **versionCode 9 / versionName 9.0**: main commit `2a966b48329f80caa1aef2777c335ba088038f50`. The successful archived [Build Android App #79](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/39577948457) provides `app-debug.apk` and signed `app-release.aab`. These are not the same kind of signature: debug APK vs upload-signed AAB.
- First downloaded native release candidate **versionCode 12 / versionName 12.0**, commit `3567e9f3b10c1c886fa9243b13758a03f4624e3d`: [Native signed candidate](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45818911552). Newer branch code may not be present in this pinned artifact.
- Later [signed-candidate verification run](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45824452644) **passed**, including an automated comparison with the historical version 9 AAB, correct package name and version-code progression, and Android Build Tools' 16 KB ZIP alignment check.
- Both signed **AAB upload-key** certificates have fingerprint `F8:15:1D:63:05:F2:94:67:B2:EB:D4:F7:6F:DF:7F:B2:2E:22:92:D3:DB:A6:7C:1F:8A:DE:B5:DD:7D:C7:66:29`. This does **not** establish that the Play-distributed APKs have the same signer; Play App Signing can use a distinct key.
- Verified application ID: `com.awisogera.votingpostermaker`; old build versionCode 9 and native release candidate versionCode 12. Current production `main` versionCode is 11, so check Play Console's **highest-ever uploaded** versionCode independently before release.

## Runtime evidence and limitations
- The first [API 35 emulator comparison](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45824058384) installed and launched both APKs separately; [download actual installed screenshots and UI dumps from its artifact](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45824058384). The native Home screenshot showed **Voting Poster**, **Single Poster Design** and **My Designs**. The historical v9 WebView process was alive but its screenshot was **blank white**, with no visible WebView child controls in the UI hierarchy. Therefore **do not** count legacy v9 on API 35 as a completed visual or feature-parity test. The cause is unresolved; it might be a version- or emulator-specific WebView initialization issue.
- Independent local Chromium execution of **the HTML extracted from the archived version 9 APK** displayed the legacy dual-photo editor. The Background, Frame, Text, Sticker, Emoji, Canvas and 3D Backgrounds panels opened without a JavaScript page exception in those bounded checks. The visual default included the Tagda Muqabla A/B frame. The source offered two visible frames, eleven canvas sizes (including 1280 × 1100), and eight 3D text presets. A desktop Chromium preview is **not** evidence that those features work in the Android APK or that the Kotlin output matches pixels.
- Dedicated [.github/workflows/native-v9-baseline-compare.yml](../.github/workflows/native-v9-baseline-compare.yml) now compares archived version 9 and the pinned native candidate on Android API 29 and API 35, capturing screenshots, UI XML, package versions and v9 WebView diagnostics. It uses **uninstall between builds**, so it is **not an in-place upgrade** and must never be described as such.

## Partial parity matrix — runtime proof still required
| Version 9 source/runtime feature | Native source/checkpoint | What remains unverified |
| --- | --- | --- |
| A and B photo import, independent pan/zoom/replace | Kotlin photo slots, Photo Picker, layer-scoped renderer, API 35 instrumentation | Full same-device old-versus-new visual and gesture comparison |
| Legacy two visible frames and white divider | Three exact archived frame PNGs under native assets; two selectable | Pixel-perfect actual editor/export comparison |
| Fifteen background colors | Kotlin background choices, gradients and per-layer backdrops | Same image/color output against legacy source |
| Text add/edit/font/color/stroke/background, eight 3D styles | Native text layers and refined background/outline/shadow controls | Each preset/font/RTL and exported bitmap pixel comparison |
| Stickers and emoji | Kotlin glyph layers/catalogs | Tap, drag, resize, delete and final render, sizes/placement |
| Eleven canvas choices | Kotlin native size presets | Exact exported PNG dimensions and fit/framing |
| Gallery PNG save; WhatsApp/Facebook/Other sharing | Native MediaStore and Android share-sheet | Recipient-app verification and storage paths on API 24–28 |
| Original Android signing identity and store upgrade | Same GitHub AAB upload-key fingerprint; native code 12 versus old 9/current main 11 | Actual Play Console version, app-signing lineage, upload acceptance and tested Play update |

## Release decision
**Do not merge**, remove Capacitor source or claim Google Play acceptance until full legacy parity, native editor tests, current-head signed AAB/APK checks and the Play-distributed in-place update are independently verified. The user prioritizes obtaining an AAB that the Play Console will accept; a matching GitHub upload key, version increase and 16 KB alignment greatly reduce technical risks but cannot bypass Play Console validation or policy review. Keep the present icon byte-for-byte unchanged; never modify Voting-Pro.
