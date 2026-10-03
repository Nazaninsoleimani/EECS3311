#!/usr/bin/env bash
# Regenerates all UML diagrams (PNG) from the PlantUML sources.
# Requires Java and plantuml.jar (https://plantuml.com/download).
# Usage: scripts/render-diagrams.sh [path/to/plantuml.jar]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$ROOT/plantuml.jar}"
cd "$ROOT/docs/diagrams"
java -DPLANTUML_LIMIT_SIZE=20000 -jar "$JAR" -tpng -o ../stage1/img ./*.puml
echo "Diagrams written to docs/stage1/img/"
