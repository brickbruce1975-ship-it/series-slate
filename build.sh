#!/usr/bin/env bash
set -eu
mkdir -p build
kotlinc src/*.kt -jvm-target 17 -include-runtime -d build/series-slate.jar
kotlinc tests/*.kt -jvm-target 17 -classpath build/series-slate.jar -d build/verification.jar
java -cp "build/series-slate.jar:build/verification.jar" slate.VerifyKt
java -jar build/series-slate.jar --demo
