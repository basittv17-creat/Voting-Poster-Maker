#!/usr/bin/env bash
set -euo pipefail

adb install -r test-apks/app-debug.apk
adb install -r test-apks/app-debug-androidTest.apk
adb shell am instrument -w com.awisogera.votingpostermaker.test/androidx.test.runner.AndroidJUnitRunner |
  tr -d '\r' | tee instrumentation-results.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)$' instrumentation-results.txt
