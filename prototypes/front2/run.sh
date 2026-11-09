#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p build
javac -encoding UTF-8 -d build src/com/oopteama/chess/ui/*.java src/com/oopteama/chess/demo/*.java
java -cp build com.oopteama.chess.demo.Front2Demo
