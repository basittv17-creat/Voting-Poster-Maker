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
