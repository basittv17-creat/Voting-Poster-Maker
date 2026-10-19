import os
import json
import re

print("="*50)
print("  VOTING POSTER MAKER - FULL PROJECT AUDIT")
print("="*50)

# 1. Check root configuration files
print("\n[1] Checking Root Files...")
root_files = os.listdir(".")
print("Root files found:", [f for f in root_files if not f.startswith(".")])

if "package.json" in root_files:
    with open("package.json", "r") as f:
        pkg = json.load(f)
    print(f" -> App Name (package.json): {pkg.get('name')}")
    print(f" -> Version: {pkg.get('version')}")

if "capacitor.config.json" in root_files:
    with open("capacitor.config.json", "r") as f:
        cap = json.load(f)
    print(f" -> Capacitor App ID (Package Name): {cap.get('appId')}")
    print(f" -> Capacitor App Name: {cap.get('appName')}")

# 2. Check Android Gradle configurations and SDK versions
print("\n[2] Checking Android & Gradle Configurations...")
gradle_paths = []
for root, dirs, files in os.walk("android"):
    for file in files:
        if file.endswith(".gradle") or file == "AndroidManifest.xml" or file == "gradle.properties":
            gradle_paths.append(os.path.join(root, file))

for path in gradle_paths:
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
    
    # Check minSdkVersion
    if "minSdkVersion" in content:
        matches = re.findall(r"(minSdkVersion\s*[\s=]+\s*\d+)", content)
        print(f" -> [{path}] MinSDK found: {matches}")
    
    # Check package name in Manifest
    if "package=" in content or "applicationId" in content:
        matches = re.findall(r"(applicationId\s*[\s=]+\s*[\"\'].*?[\"\']|package\s*=.*?[\"\'].*?[\"\'])", content)
        if matches:
            print(f" -> [{path}] IDs/Packages: {matches}")

# 3. Check GitHub Actions Workflow
print("\n[3] Checking GitHub Actions Workflow...")
workflow_path = ".github/workflows/build-android.yml"
if os.path.exists(workflow_path):
    with open(workflow_path, "r", encoding="utf-8") as f:
        wf_content = f.read()
    print(" -> Workflow file exists.")
    if "bundleRelease" in wf_content and "store.file" in wf_content:
        print(" -> Signing parameters and bundleRelease are correctly configured in workflow.")
    else:
        print(" -> WARNING: Signing parameters might be missing in workflow!")
else:
    print(" -> WARNING: GitHub workflow file not found!")

print("\n" + "="*50)
print("  AUDIT COMPLETED SUCCESSFULLY")
print("="*50)
