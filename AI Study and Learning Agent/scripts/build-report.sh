#!/usr/bin/env bash
# Rebuilds docs/stage2/stage2-report.pdf from docs/stage2/stage2-report.html (edit the HTML, then run this).
# Regenerates the overview class diagrams first. Needs python3 and Google Chrome (or Chromium).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 "$ROOT/docs/stage2/diagrams/overview_svg.py" >/dev/null
CHROME="${CHROME:-}"
for c in "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" "$(command -v google-chrome || true)" "$(command -v chromium || true)"; do
  if [ -z "$CHROME" ] && [ -n "$c" ] && [ -x "$c" ]; then CHROME="$c"; fi
done
[ -n "$CHROME" ] || { echo "Chrome not found; set CHROME=/path/to/chrome" >&2; exit 1; }
# rasterise the rotated diagrams at 2x so the PDF shows them sharp and at a predictable size
for svg in "$ROOT"/docs/stage2/img/*-portrait.svg; do
  read -r w h < <(python3 -c "import re,sys;m=re.search(r'viewBox=\"0 0 (\d+) (\d+)\"',open(sys.argv[1]).read());print(m.group(1),m.group(2))" "$svg")
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 --window-size="$w,$h" \
    --screenshot="${svg%.svg}.png" "file://$svg" >/dev/null 2>&1
done
"$CHROME" --headless=new --disable-gpu --no-pdf-header-footer \
  --print-to-pdf="$ROOT/docs/stage2/stage2-report.pdf" "file://$ROOT/docs/stage2/stage2-report.html" 2>/dev/null
echo "Wrote docs/stage2/stage2-report.pdf"
