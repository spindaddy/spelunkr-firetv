#!/bin/sh
set -e
cd "$(dirname "$0")"

if [ -z "$JAVA_HOME" ]; then
  if [ -x "$HOME/Library/Java/temurin-17/bin/java" ]; then
    JAVA_HOME="$HOME/Library/Java/temurin-17"
    export JAVA_HOME
  fi
fi

if [ -z "$ANDROID_HOME" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then
    ANDROID_HOME="$HOME/Library/Android/sdk"
    export ANDROID_HOME
  fi
fi

if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "JDK 17 is required. Set JAVA_HOME." >&2
  exit 1
fi

if [ ! -d "$ANDROID_HOME/platforms/android-34" ]; then
  echo "Android SDK platform 34 is required. Set ANDROID_HOME." >&2
  exit 1
fi

printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

KEYSTORE_DIR="keystore"
mkdir -p "$KEYSTORE_DIR"
if [ ! -f "$KEYSTORE_DIR/spelunkr-firetv.jks" ]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -v \
    -keystore "$KEYSTORE_DIR/spelunkr-firetv.jks" \
    -alias spelunkr \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass spelunkr -keypass spelunkr \
    -dname "CN=SPELUNKR, OU=FireTV, O=SPELUNKR, C=US"
fi

cat > "$KEYSTORE_DIR/keystore.properties" <<'EOF'
storeFile=keystore/spelunkr-firetv.jks
storePassword=spelunkr
keyAlias=spelunkr
keyPassword=spelunkr
EOF

GRADLE="$HOME/Library/Android/gradle/gradle-8.7/bin/gradle"
if [ ! -x "$GRADLE" ]; then
  GRADLE="gradle"
fi

"$GRADLE" assembleRelease

mkdir -p ../dist
cp -f app/build/outputs/apk/release/app-release.apk ../dist/SPELUNKR-FireTV.apk
echo "APK: $(cd .. && pwd)/dist/SPELUNKR-FireTV.apk"
