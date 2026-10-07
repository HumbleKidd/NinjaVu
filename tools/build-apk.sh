#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/tmp/android-sdk}}"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"
BUILD="$ROOT/build/apk"
KEY="$ROOT/keystore/ninjavu-release.jks"
OUT="$ROOT/dist/NinjaVu-1.1.0.apk"

if [[ ! -f "$PLATFORM" ]]; then
  echo "Android SDK platform 34 not found at $SDK"
  echo "Install with: sdkmanager \"platforms;android-34\" \"build-tools;34.0.0\""
  exit 1
fi

rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$ROOT/dist"

echo "Compiling resources"
"$BT/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$BUILD/resources.zip"
"$BT/aapt2" link \
  -o "$BUILD/base.apk" \
  -I "$PLATFORM" \
  --manifest "$ROOT/app/src/main/AndroidManifest.xml" \
  --java "$BUILD/gen" \
  --min-sdk-version 24 \
  --target-sdk-version 34 \
  --version-code 2 \
  --version-name 1.1.0 \
  --auto-add-overlay \
  -R "$BUILD/resources.zip"

echo "Compiling Java"
find "$ROOT/app/src/main/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.list"
javac --release 17 -classpath "$PLATFORM" -d "$BUILD/classes" @"$BUILD/sources.list"

echo "Dexing"
find "$BUILD/classes" -name '*.class' > "$BUILD/classlist.txt"
mapfile -t CLASS_FILES < "$BUILD/classlist.txt"
"$BT/d8" --min-api 24 --output "$BUILD" "${CLASS_FILES[@]}"

python3 - "$BUILD" "$ROOT" << 'PYZIP'
import shutil, sys, zipfile
from pathlib import Path
build, root = Path(sys.argv[1]), Path(sys.argv[2])
unsigned = build / "unsigned.apk"
shutil.copy(build / "base.apk", unsigned)
with zipfile.ZipFile(unsigned, "a") as z:
    z.write(build / "classes.dex", "classes.dex")
    assets = root / "app/src/main/assets"
    if assets.exists():
        for f in assets.rglob("*"):
            if f.is_file():
                z.write(f, "assets/" + str(f.relative_to(assets)))
PYZIP

echo "Aligning and signing"
"$BT/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
"$BT/apksigner" sign \
  --ks "$KEY" \
  --ks-pass pass:NinjaVuRelease2026 \
  --key-pass pass:NinjaVuRelease2026 \
  --out "$OUT" \
  "$BUILD/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT"
echo "Wrote $OUT"
