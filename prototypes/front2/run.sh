#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p build
javac -encoding UTF-8 -d build src/com/oopteama/chess/proto/front2/*.java
java -cp build com.oopteama.chess.proto.front2.Front2Demo
