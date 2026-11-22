#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 2 ]]; then
  echo "Usage: $0 trusted-installed-release.apk signed-native-release.apk" >&2
  exit 2
fi

package_info() {
  aapt dump badging "$1" | awk '/^package:/ { print; found=1 } END { if (!found) exit 1 }'
}

signers() {
  apksigner verify --print-certs "$1" |
    awk '/^Signer #[0-9]+ certificate SHA-256 digest:/ { print $NF; found=1 } END { if (!found) exit 1 }' |
    sort
}

old=$(package_info "$1")
new=$(package_info "$2")
expected="name='com.awisogera.votingpostermaker'"
if [[ "$old" != *"$expected"* || "$new" != *"$expected"* ]]; then
  echo "Unexpected app ID in an input APK." >&2
  exit 1
fi
old_version=$(sed -n "s/.*versionCode='\([0-9][0-9]*\)'.*/\1/p" <<< "$old")
new_version=$(sed -n "s/.*versionCode='\([0-9][0-9]*\)'.*/\1/p" <<< "$new")
if [[ -z "$old_version" || -z "$new_version" ]] || (( new_version <= old_version )); then
  echo "The native APK must have a higher version code." >&2
  exit 1
fi
old_signers=$(signers "$1")
new_signers=$(signers "$2")
if [[ "$old_signers" != "$new_signers" ]]; then
  echo "Signer fingerprints differ. Verify a documented signing lineage separately." >&2
  exit 1
fi

echo "APK identity, version increase and matching signer fingerprints verified."
