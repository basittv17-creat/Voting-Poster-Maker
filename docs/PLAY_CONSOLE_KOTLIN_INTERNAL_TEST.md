# Play Console acceptance test before merging Kotlin into main

Owner-approved strategy: keep the existing `main` Capacitor app unchanged. Produce the **native Kotlin + Compose signed AAB** from `feature/native-kotlin-compose-rebuild`. Upload it as a **new Internal testing release** in the existing Voting Poster Maker Play Console app, not as a production rollout or a new Play app. If Play accepts the bundle, verify its generated Play-signed APK on an actual test install, then finish/verify parity, native release workflow, and only then merge the same validated source into `main`.

## Verified technical baseline (26 November 2026)
- Existing Android application ID: `com.awisogera.votingpostermaker`, same in original version 9/11 and Kotlin version 12.
- Existing `main` release versionCode 11, versionName 10.1. Current Kotlin candidate versionCode 12, versionName 12.0. **Check the highest version code already uploaded to Play Console**, including all active/inactive tracks; select a new unused greater code if needed.
- Original owner-selected launcher icon in `main` and Kotlin native source is the same Git blob `e4686325e1d2364e9bbc81c091e265bb674b4bfa`; the native launcher and roundIcon refer to the exact owner-selected `voting_poster_icon.webp` asset.
- Historical version 9 signed AAB and new Kotlin signed AAB match **upload certificate** SHA-256 `F8:15:1D:63:05:F2:94:67:B2:EB:D4:F7:6F:DF:7F:B2:2E:22:92:D3:DB:A6:7C:1F:8A:DE:B5:DD:7D:C7:66:29`.
- [Native signed candidate release check](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/45824452644) verified package identity, APK alignment and historical upload signer. This was an older snapshot; before upload **use only a signed candidate built from the exact intended source commit**, not a stale artifact.
- Kotlin targetSdk 36, minSdk 24. Google Play currently requires target API 36 for new standard Android app updates submitted since August 31, 2026 (exceptions for other form factors).

## Exact steps before uploading
1. Check [PR #1](https://github.com/basittv17-creat/Voting-Poster-Maker/pull/1), current source commit, its native Build/Unit/Lint/Offline Android tests and signed-candidate job. Resolve all app-critical failures. Signed AAB artifacts are GitHub Actions ZIP downloads containing `bundle/release/app-release.aab`; corresponding Android testable APK is `apk/release/app-release.apk`.
2. Confirm the **same Git commit SHA** for source/build evidence and release AAB. If a newer Kotlin feature or bug fix has landed, regenerate the signed AAB. Any subsequent Play upload after versionCode 12 has been consumed must use a higher versionCode (13 or above based on the real Play history).
3. In Play Console open the **existing** Voting Poster Maker listing and `Test and release > Testing > Internal testing`. Create a new release and add the native `app-release.aab`. Do not upload a debug APK, old Capacitor AAB, or a ZIP file.
4. Review validation messages: package-name mismatch, upload certificate mismatch, duplicate/insufficient versionCode, target API, ABI/device coverage, 16-KB page support, manifest permissions/privacy, Data safety and other Play policies. Fix causes in the **Kotlin branch** and regenerate a **new** higher-code signed AAB as necessary. Never falsify metadata to bypass validation.
5. If uploaded/accepted, release to **internal testers only**. Test upgrades using the Play-distributed version on a trusted test device (or downloaded Play-signed APK), compare original poster/gallery data, functionality, offline flows, photo picker, Urdu text, exports and share. The repository upload-signing certificate can differ from Play's app-signing certificate.
6. Only after legacy feature parity, requested features, Android matrix, latest signed source and real upgrade checks pass, switch the canonical native build to `main` and merge PR. Preserve old release history and do not retire Capacitor until the native-only main build is verified.

## Clarification
Google Play does **not** require the new UI or every function to look identical to the prior app. For an update it chiefly checks the existing package identity, a higher unused versionCode, accepted signing and the current technical/policy conditions. Feature parity is this project's separate owner requirement, not evidence of Play acceptance.

Official reference pages:
- https://support.google.com/googleplay/android-developer/answer/9859350 (update requirements)
- https://support.google.com/googleplay/android-developer/answer/9842756 (Play app signing; upload key vs distribution key)
- https://support.google.com/googleplay/android-developer/answer/9859348 (testing tracks/release procedure)
- https://developer.android.com/google/play/requirements/target-sdk (2026 API requirement)
