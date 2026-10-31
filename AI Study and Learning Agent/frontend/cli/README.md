# CLI front end (package `ca.yorku.eecs3311.studyagent.presentation.cli`)

Command-line interface invoked as `study <group> <action> [options]`. `CLIApplication` parses arguments with `CommandParser` into a `ParsedCommand` and dispatches to the same `LearningFacade` operations the GUI uses (see SD08 and SD11).

> Implemented in Stage 2. The table lists the classes of the Stage 1 class model (`docs/diagrams/class-model.iuml`) that live here; the Java sources are in this folder.

| Class | Kind | Notes |
|---|---|---|
| `CLIApplication` | class |  |
| `CommandParser` | class |  |
| `ParsedCommand` | class |  |
| `StudyCommands` | class | added in Stage 2: the F06-F16 command handlers, split out of `CLIApplication` |

## Commands

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

Also available: `study help`, `study status` (LLM in use and token count), `study quiz list`, `study quiz generate ... --take` (answer immediately), `study flashcards list`, `study study-plan show --all`, `study reminder list [--all]` and `study reminder dispatch`. `study quiz explain` accepts the question number shown after grading. Add `--help` to any command for its usage and `--trace` to print the agent's plan and step results. Running `study` with no arguments opens an interactive shell.
