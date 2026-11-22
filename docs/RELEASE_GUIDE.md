# Release gate

`main` continues producing the existing Capacitor release. The independent native PR check builds a debug APK and runs document tests. Keep the migration in draft until legacy parity, device verification, and the signing upgrade have been exercised.

The native application uses version code 11 and version name 11.0. Before cutover, compare version code with the latest Play build, configure the authorized release signing path, verify the production certificate lineage, and test a signed upgrade on-device. Verify the AAB signature before distribution. Keep signing configuration and material outside source control.

## Verify a native release candidate

1. From a trusted release workstation, supply an absolute path to a private Java properties file through `VPM_RELEASE_SIGNING_FILE`. The file has `storeFile`, `storePassword`, `keyAlias` and `keyPassword` entries. A relative `storeFile` is resolved next to the properties file. Keep both files outside the checkout. Pull request CI uses only debug signing.
2. Run `./gradlew --no-daemon :app:assembleRelease :app:bundleRelease` from `native-android`. When using Play App Signing, compare installed APKs from Play distribution. An upload-key signature is not the installed app's signer.
3. Put Android Build Tools on PATH and run `bash scripts/check-release-upgrade.sh legacy.apk native.apk`. It verifies both signatures, identity, version increase, and identical current signers. If the app's key was rotated, verify the authorized lineage separately.
4. On a dedicated device, install the trusted legacy release, save a poster and export it. Install the matching production-signed native APK in place using `adb install -r`, preserving installed app data. Confirm launch, access to existing gallery outputs, and a new native save/export. For Play or split APKs, use a trusted internal track and verify the update in place.

[Android app signing](https://developer.android.com/studio/publish/app-signing) · [apksigner](https://developer.android.com/tools/apksigner)
