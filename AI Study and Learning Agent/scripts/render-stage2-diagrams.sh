#!/usr/bin/env bash
# Renders the complete Stage 2 PlantUML class model (all classes and methods) to PNG.
# Requires Java and plantuml.jar (https://plantuml.com/download); no Graphviz needed (smetana layout).
# Usage: scripts/render-stage2-diagrams.sh [path/to/plantuml.jar]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$ROOT/plantuml.jar}"
cd "$ROOT/docs/stage2/diagrams"
java -Djava.awt.headless=true -DPLANTUML_LIMIT_SIZE=30000 -jar "$JAR" -tpng -o ../img/full ./*.puml
echo "Diagrams written to docs/stage2/img/full/"
