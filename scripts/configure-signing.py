import os
import base64
import sys
from pathlib import Path

root = Path("android/app")
gradle = root / "build.gradle"

names = [
    "ANDROID_KEYSTORE_BASE64",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
]
missing = [name for name in names if not os.getenv(name)]

if missing:
    print("Missing signing secrets:", ", ".join(missing))
    sys.exit(1)

if not gradle.exists():
    print("ERROR: android/app/build.gradle was not found.")
    sys.exit(1)

try:
    key_data = base64.b64decode(
        os.environ["ANDROID_KEYSTORE_BASE64"],
        validate=True
    )
except Exception:
    print("ERROR: KEYSTORE_BASE64 is not valid Base64.")
    sys.exit(1)

keystore = root / "upload-keystore.jks"
keystore.write_bytes(key_data)

text = gradle.read_text()

start = text.find("    signingConfigs {")
if start >= 0:
    depth = 0
    end = None
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        print("ERROR: Could not parse signingConfigs block.")
        sys.exit(1)
    text = text[:start] + text[end:]

block = """    signingConfigs {
        release {
            storeFile file('upload-keystore.jks')
            storeType 'PKCS12'
            storePassword System.getenv('ANDROID_KEYSTORE_PASSWORD') ?: ''
            keyAlias System.getenv('ANDROID_KEY_ALIAS') ?: ''
            keyPassword System.getenv('ANDROID_KEY_PASSWORD') ?: ''
        }
    }

"""

marker = "    buildTypes {"
if marker not in text:
    print("ERROR: buildTypes block was not found.")
    sys.exit(1)

text = text.replace(marker, block + marker, 1)

release_marker = "        release {"
pos = text.find(release_marker, text.find("    buildTypes {"))
if pos < 0:
    print("ERROR: release build type was not found.")
    sys.exit(1)

insert_at = pos + len(release_marker)
text = text[:insert_at] + "\n            signingConfig signingConfigs.release" + text[insert_at:]

gradle.write_text(text)
print("Release signing configuration updated.")
