"""Configure release signing from GitHub Actions secrets if all are present.

Expected repository secrets:
ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD,
ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD.
If any are missing, Gradle produces unsigned release artifacts for testing only.
"""
import os
from pathlib import Path

root = Path("android/app")
gradle = root / "build.gradle"
values = {
    "ANDROID_KEYSTORE_BASE64": os.getenv("ANDROID_KEYSTORE_BASE64", ""),
    "ANDROID_KEYSTORE_PASSWORD": os.getenv("ANDROID_KEYSTORE_PASSWORD", ""),
    "ANDROID_KEY_ALIAS": os.getenv("ANDROID_KEY_ALIAS", ""),
    "ANDROID_KEY_PASSWORD": os.getenv("ANDROID_KEY_PASSWORD", ""),
}
if not all(values.values()):
    print("Signing secrets not all available; release builds will be unsigned. Configure the four documented GitHub secrets for signed Play Store builds.")
    raise SystemExit(0)

import base64
keystore = root / "upload-keystore.jks"
keystore.write_bytes(base64.b64decode(values["ANDROID_KEYSTORE_BASE64"]))
text = gradle.read_text()
if "signingConfigs {" not in text:
    text = text.replace(
        "    buildTypes {",
        "    signingConfigs {\n"
        "        release {\n"
        "            storeFile file('upload-keystore.jks')\n"
        "            storePassword System.getenv('ANDROID_KEYSTORE_PASSWORD')\n"
        "            keyAlias System.getenv('ANDROID_KEY_ALIAS')\n"
        "            keyPassword System.getenv('ANDROID_KEY_PASSWORD')\n"
        "        }\n"
        "    }\n\n    buildTypes {",
        1,
    )
text = text.replace(
    "    buildTypes {\n        release {",
    "    buildTypes {\n        release {\n            signingConfig signingConfigs.release",
    1,
)
gradle.write_text(text)
print("Release signing configured from GitHub Actions secrets.")
