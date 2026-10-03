#!/usr/bin/env bash
# Export every UMLet diagram to an image or PDF.
# Usage: scripts/export-umlet.sh path/to/umlet.jar [png|pdf|svg|jpg]
set -euo pipefail
JAR="${1:?usage: $0 path/to/umlet.jar [png|pdf|svg|jpg]}"
FORMAT="${2:-png}"
DIR="$(cd "$(dirname "$0")/.." && pwd)/docs/diagrams/umlet"
OUT="$DIR/$FORMAT"
mkdir -p "$OUT"
for f in "$DIR"/*.uxf; do
  name="$(basename "$f" .uxf)"
  java -jar "$JAR" -action=convert -format="$FORMAT" -filename="$f" -output="$OUT/$name"
  echo "exported $name.$FORMAT"
done
