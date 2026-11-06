#!/usr/bin/env bash
# CLI walkthrough over the sample data, for testing and for the demonstration videos.
# Uses its own database (build/demo/study-agent.db) so your real data is untouched.
# Usage: scripts/demo-cli.sh            (AI features need a key in .env)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./gradlew -q :frontend:cli:installDist
export STUDY_AGENT_DB="$ROOT/build/demo/study-agent.db"
rm -f "$STUDY_AGENT_DB"
study() { "$ROOT/frontend/cli/build/install/study/bin/study" "$@"; }
# keep going after a failing step: showing error handling is part of the demo
step() { printf '\n\033[1m$ study %s\033[0m\n' "$*"; study "$@" || true; }

EXAM="$(date -v+20d +%F 2>/dev/null || date -d '+20 days' +%F)"
step status
step course create --code EECS3311 --title "Software Design" --exam "$EXAM"
for f in sample-data/EECS3311/*.md; do step material upload --course EECS3311 "$f"; done
step material list --course EECS3311
step ask --course EECS3311 "What is the difference between the Strategy and State patterns?" --trace
step study-plan generate --course EECS3311 --minutes 60
step quiz generate --course EECS3311 --count 3 --types mcq,tf
step flashcards generate --course EECS3311 --topic "observer pattern" --count 3
step concept-map show --course EECS3311
step progress show --course EECS3311
step history show --course EECS3311 --last 1d
echo
echo "Interactive commands to try next: study quiz take <quizId>, study flashcards review --course EECS3311,"
echo "study learning-session start --course EECS3311 (set STUDY_AGENT_DB=$STUDY_AGENT_DB to use this demo data)."
