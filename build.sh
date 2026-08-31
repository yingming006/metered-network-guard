#!/usr/bin/env bash
# 在受限/无 root 的 WSL 环境下构建 APK
set -e
BASE="${BASE:-$(pwd)/.android-tools}"
export JAVA_HOME="$BASE/jdk"
export ANDROID_HOME="$BASE/sdk"
export ANDROID_SDK_ROOT="$BASE/sdk"
export GRADLE_USER_HOME="$BASE/gradle-home"
export HOME="$BASE/userhome"
export ANDROID_USER_HOME="$BASE/userhome/.android"
# -XX:-UseContainerSupport 规避 JDK17.0.2 在 cgroup v2 下的 NPE 构建故障
export JAVA_TOOL_OPTIONS="-Duser.home=$BASE/userhome -Djava.io.tmpdir=$BASE/tmp -Dorg.gradle.daemon=false -XX:-UseContainerSupport"
mkdir -p "$BASE/tmp" "$BASE/userhome/.android"
DIR_OF_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR_OF_SCRIPT/NetworkGuard"
./gradlew :app:assembleDebug "$@"
echo
echo "APK 位置: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
