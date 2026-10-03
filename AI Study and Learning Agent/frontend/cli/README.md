# CLI front end (planned package `ca.yorku.eecs3311.studyagent.presentation.cli`)

Command-line interface invoked as `study <group> <action> [options]`. `CLIApplication` parses arguments with `CommandParser` into a `ParsedCommand` and dispatches to the same `LearningFacade` operations the GUI uses (see SD08 and SD11).

> Stage 1 is the design stage, so this folder contains no implementation yet. The classes listed below come from the Stage 1 class model (`docs/diagrams/class-model.iuml`) and will be implemented here in Stage 2.

| Class | Kind | Notes |
|---|---|---|
| `CLIApplication` | class |  |
| `CommandParser` | class |  |
| `ParsedCommand` | class |  |

## Planned commands

| Command | Feature |
|---|---|
| `study course create --code <c> --title <t> [--exam <yyyy-mm-dd>]` / `list` / `update <id> ...` / `delete <id>` | F01 |
| `study material upload --course <c> <file>` / `list --course <c>` / `delete <id>` | F02 |
| `study material summarize <id> [--length brief\|standard\|detailed]` | F03 |
| `study material extract-concepts <id>` | F04 |
| `study ask --course <c> "<question>"` / `study ask --course <c> -i` (interactive) | F05 |
| `study study-plan generate --course <c> --exam <date> --minutes <n> [--focus a,b]` / `show` / `done <taskId>` | F06 |
| `study flashcards generate --course <c> --topic <t> --count <n>` / `review --course <c>` | F07 |
| `study quiz generate --course <c> --concepts a,b --count <n> [--types mcq,tf,short] [--difficulty <level>]` | F08 |
| `study quiz take <quizId>` (interactive; graded on completion) | F09 |
| `study quiz explain <attemptId> <questionId>` | F10 |
| `study weak-topics show --course <c>` | F11 |
| `study learning-session start --course <c>` (interactive) | F12 |
| `study progress show --course <c>` | F13 |
| `study reminder add --at "<datetime>" [--repeat daily\|weekly] "<message>"` / `list` / `cancel <id>` | F14 |
| `study concept-map show --course <c> [--format text\|mermaid]` | F15 |
| `study history show [--course <c>] [--last 7d] [--type <eventType>]` | F16 |
