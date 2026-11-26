#!/usr/bin/env bash
set -euo pipefail

adb install -r test-apks/app-debug.apk
adb install -r test-apks/app-debug-androidTest.apk
# The emulator UI tests need a foreground, unlocked display after boot.
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard

capture_failure() {
  local result=$?
  if (( result != 0 )); then
    adb exec-out run-as com.awisogera.votingpostermaker cat cache/navigation-failure.png > navigation-failure.png || true
    adb shell dumpsys activity activities > failure-activities.txt || true
    adb logcat -d -t 400 > failure-logcat.txt || true
  fi
}
trap capture_failure EXIT

adb shell am instrument -w com.awisogera.votingpostermaker.test/androidx.test.runner.AndroidJUnitRunner |
  tr -d '\r' | tee instrumentation-results.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)$' instrumentation-results.txt
