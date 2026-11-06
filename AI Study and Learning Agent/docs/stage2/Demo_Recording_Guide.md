# Demonstration recording guide (Stage 2, item 15)

Every feature needs a video segment that shows: the feature ID and name, how it is invoked and its input, the running result, where AI is involved, and an error or alternative case. Collectively the videos must show both the GUI and the CLI. Put each link and timestamp in section 7 of `stage2-report.html`, then run `scripts/build-report.sh`.

## Before recording

1. Put your Gemini key in `.env` (`cp .env.example .env`). **Never show `.env` on screen.**
2. Use a clean demo database so old data does not appear: `export STUDY_AGENT_DB="$PWD/build/demo/study-agent.db"` before starting the GUI or CLI (`./gradlew :frontend:gui:run`, `frontend/cli/build/install/study/bin/study`).
3. Use the files in `sample-data/EECS3311/`.
4. Say the feature ID and name at the start of each segment (or show a title card).

A single 20-30 minute recording with chapters works; give each feature a timestamped link (for YouTube: `https://youtu.be/<id>?t=<seconds>`). Make the videos public or "unlisted", not private.

## Suggested segments

| # | Feature | Interface | Steps | Error / alternative to show | Where AI is involved |
|---|---|---|---|---|---|
| 1 | F01 Course Management | GUI, then CLI | Courses tab: create EECS3311 with an exam ~20 days ahead; edit the title; `study course list` | Create the same code again (rejected); exam date in the past (rejected); delete asks for confirmation | None |
| 2 | F02 Material Upload | GUI | Materials: Upload the three sample files; show the status table | Upload a `.pptx` (rejected) or an empty `.txt` (FAILED with reason) | Embeddings only |
| 3 | F04 Concept Extraction | GUI + CLI | Point at the concepts listed after upload; `study material extract-concepts <id>` | Mention that relations to unknown concepts are dropped | LLM extracts concepts and relations |
| 4 | F03 Summarization | GUI | Select week 6, length Brief, Summarize; then Detailed | - | LLM writes the summary (map-reduce for long files) |
| 5 | F05 Question Answering | GUI, then `study ask -i` | Ask "What is the difference between Strategy and State?", then "why is that?"; tick "Show agent plan" | Ask something unrelated ("What is photosynthesis?") -> not covered, no LLM call | Planner, retrieval, cited answer, grounding check |
| 6 | F08 Quiz Generation | GUI | Quiz tab: 5 questions, all types, Intermediate, Generate | Point out the notice if fewer questions passed validation | LLM writes specs; factories validate |
| 7 | F09 Grading | GUI, then `study quiz take <id>` | Answer, leaving one blank; Submit; show per-question feedback | Blank answer scored 0; short answer graded against the rubric | LLM grades short answers only |
| 8 | F10 Explanation | GUI | Click Explain on a wrong answer | `study quiz explain <attemptId> 2` in the CLI | LLM explains with sources |
| 9 | F13 Progress | GUI | Keep the Progress tab open while submitting a quiz: it updates live (Observer) | Empty state on a new course | None |
| 10 | F11 Weak Topics | GUI + CLI | Analyze weak topics before and after enough practice (5+ graded answers) | "Not enough graded practice" message | LLM diagnoses misconceptions |
| 11 | F12 Adaptive Difficulty | CLI `study learning-session start --course EECS3311` | Answer 3 correctly -> level up; 2 wrong -> level down | Type `q` to end; session summary with level history | LLM writes each question; State decides level |
| 12 | F06 Study Plan | GUI | Study Plan: minutes 60, focus "observer pattern", Generate; read the strategy and rationale; mark a task done | Then do badly on a quiz about a planned concept and show the revised plan (revision number) | LLM prioritises and writes objectives; Strategy schedules |
| 13 | F07 Flashcards | CLI, then GUI | `study flashcards generate --course EECS3311 --topic "observer pattern" --count 5`; `study flashcards review --course EECS3311` | Rate one card Again and show its next review date | LLM writes cards; Leitner review is deterministic |
| 14 | F14 Reminders | GUI | Reminders: add one 2 minutes from now; wait for the desktop notification | Time in the past (rejected); cancel a reminder | None |
| 15 | F15 Concept Map | GUI + CLI | Concept Map: colours by mastery, click a concept, Quiz me on this; `study concept-map show --course EECS3311 --format mermaid` | Empty state on a course without concepts | Relations came from the LLM in F04 |
| 16 | F16 History | GUI + CLI | History tab: filter by activity type; `study history show --last 7d --type quiz_completed` | "No matching history" with a narrow filter | None |
