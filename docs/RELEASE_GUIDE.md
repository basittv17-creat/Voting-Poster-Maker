# Release gate

`main` continues producing the existing Capacitor release. The independent native PR check builds a debug APK and runs document tests. Keep the migration in draft until legacy parity, device verification, and the signing upgrade have been exercised.

The native application uses version code 12 and version name 12.0. Before cutover, compare version code with the latest Play build, configure the authorized release signing path, verify the production certificate lineage, and test a signed upgrade on-device. Verify the AAB signature before distribution. Keep signing configuration and material outside source control.

## Verify a native release candidate

1. From a trusted release workstation, supply an absolute path to a private Java properties file through `VPM_RELEASE_SIGNING_FILE`. The file has `storeFile`, `storePassword`, `keyAlias` and `keyPassword` entries. A relative `storeFile` is resolved next to the properties file. Keep both files outside the checkout. Pull request CI uses only debug signing.
2. Run `./gradlew --no-daemon :app:assembleRelease :app:bundleRelease` from `native-android`. When using Play App Signing, compare installed APKs from Play distribution. An upload-key signature is not the installed app's signer.
3. Put Android Build Tools on PATH and run `bash scripts/check-release-upgrade.sh legacy.apk native.apk`. It verifies both signatures, identity, version increase, and identical current signers. If the app's key was rotated, verify the authorized lineage separately.
4. On a dedicated device, install the trusted legacy release, save a poster and export it. Install the matching production-signed native APK in place using `adb install -r`, preserving installed app data. Confirm launch, access to existing gallery outputs, and a new native save/export. For Play or split APKs, use a trusted internal track and verify the update in place.

[Android app signing](https://developer.android.com/studio/publish/app-signing) · [apksigner](https://developer.android.com/tools/apksigner)


The historical Capacitor [signed release AAB artifact](https://github.com/basittv17-creat/Voting-Poster-Maker/actions/runs/39579485244/artifacts/12335498184) is available as an upload-signing reference. Production upgrade verification remains open. The release owner must configure `VPM_RELEASE_SIGNING_FILE` on the trusted release workstation and provide the actual installed production baseline to the upgrade test. Keep the signing files and passwords there and record only public certificate fingerprints and test results.


## Preserve the current Google Play launcher icon (26 Nov 2026)
The latest existing Capacitor `main` uses `android/app/src/main/res/drawable-nodpi/voting_poster_icon.webp` (blob `e4686325e1d2364e9bbc81c091e265bb674b4bfa`) as both `android:icon` and `android:roundIcon`. The native project copies this **exact blob** to `native-android/app/src/main/res/drawable-nodpi/voting_poster_icon.webp` and references it for both icon attributes. Do not replace it with the earlier stock mipmap resources or generated UI mockups.

The latest installed release on `main` is versionCode **11** (versionName 10.1). Native was previously also 11; it is now **12**. This corrects the version ordering only. It does **not** verify production signing, app-upgrade compatibility, legacy data preservation, feature parity, or readiness to merge. The existing `www/index.html` still supplies legacy PNG frames during native builds; do **not** delete it or the Capacitor production build until those image assets are migrated to independent licensed assets and verified.

## Remove the legacy HTML dependency safely
The native app now bundles exact copies of the two used voting frame PNGs and the previously unused third legacy frame under `native-android/app/src/main/assets/frames/`. They were extracted byte-for-byte from the existing embedded HTML data URIs. Gradle no longer reads `../www/index.html` to build native app frames. Do not mistake this for full functional parity or production cutover; legacy Capacitor files must remain in `main` until the native replacement is fully verified and production signing/upgrade succeeds. On final cutover, archive or remove obsolete Capacitor/HTML build steps only when the native `main` workflow is proven to produce a correctly signed Android release and preserve the new owner-selected app icon.
