# Release gate

`main` continues producing the existing Capacitor release. The independent native PR check builds a debug APK and runs document tests. Keep the migration in draft until legacy parity, device verification, and the signing upgrade have been exercised.

The native application uses version code 11 and version name 11.0. Before cutover, compare version code with the latest Play build, configure the authorized release signing path, verify the production certificate lineage, and test a signed upgrade on-device. Verify the AAB signature before distribution. Keep signing configuration and material outside source control.
