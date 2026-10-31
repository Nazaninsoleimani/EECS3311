# AI Study and Learning Agent

**EECS 3311 Software Design, Fall 2026 - Course Project**
Nazanin Fatemeh Soleimani (222050579)
Repository: <https://github.com/Nazaninsoleimani/EECS3311.git>

A personalized AI learning agent for university students. It works over the student's own course materials to answer questions with source citations, summarize lectures, extract concepts, build and revise study plans, generate and grade quizzes and flashcards, identify weak topics, adapt practice difficulty, and track progress. All functionality is available through both a GUI and a CLI that share the same backend facade.

## Project status

| Stage | Content | Status |
|---|---|---|
| **Stage 1 - Design** | Project definition, 16 features, use-case model (17 use cases), class model, 6 design patterns, 11 sequence diagrams, deployment diagram, traceability | Complete: [`docs/stage1/Stage1_Report.md`](docs/stage1/Stage1_Report.md) ([PDF](docs/stage1/Stage1_Report.pdf)) |
| **Stage 2 - Implementation** | Java backend (agent, tools, services, SQLite), CLI and Swing GUI for all 16 features | In progress (due Nov 10): code in `backend/` and `frontend/`, report in `docs/stage2/` |
| Stage 3 | To be announced | Planned (`docs/stage3/`) |

## Stage 1 documents

| Document | Report sections |
|---|---|
| [Stage 1 Report (complete)](docs/stage1/Stage1_Report.md) | 1-13, Appendices A-C |
| [Architecture](docs/stage1/Architecture.md) | 1.11, 3, 10 |
| [Feature Specifications](docs/stage1/Feature_Specifications.md) | 2 |
| [Use-Case Descriptions](docs/stage1/Use_Case_Descriptions.md) | 6 |
| [Design Patterns](docs/stage1/Design_Patterns.md) | 5 |
| [Traceability Matrix and Feature Realization](docs/stage1/Traceability_Matrix.md) | 8, 9 |
| [UML diagrams (UMLet, official)](docs/diagrams/umlet/) | 3, 4, 6, 7 |
| [Report figures (UMLet exports)](docs/stage1/img/) and [PlantUML sources (reference only)](docs/diagrams/) | 3, 4, 6, 7 |

## Getting the code

```bash
git clone https://github.com/Nazaninsoleimani/EECS3311.git
```

## Repository structure

| Folder | Purpose | Design link |
|---|---|---|
| `docs/` | Stage reports, UMLet diagrams (`docs/diagrams/umlet/`), PlantUML sources and rendered diagrams | all |
| `backend/` | Core module: application, agent, agent tools, domain, integration, persistence packages | Report 3.3-3.7, class diagram views B-D |
| `frontend/gui/` | Desktop GUI (`BaseView` and 12 views) | Report 3.2, class diagram view A |
| `frontend/cli/` | Command-line interface (`study <group> <action>`) | Report 1.10, Appendix B, SD08, SD11 |
| `deployment/` | Example configuration, environment template, packaging plan | Report 3.9, deployment diagram |
| `scripts/` | `export-umlet.sh` exports the UMLet diagrams; `render-diagrams.sh` renders the PlantUML sources | - |

Each code folder has a README that lists the classes of the Stage 1 class model implemented there, plus any classes added in Stage 2.

## Diagrams

The official UML diagrams are the UMLet files in [`docs/diagrams/umlet/`](docs/diagrams/umlet/). Open them in UMLet 15.1 or export them all with:

```bash
scripts/export-umlet.sh path/to/umlet.jar png
```

The PlantUML sources describe the same model. All class diagrams are generated from one shared model (`docs/diagrams/class-model.iuml`), so the views always stay consistent.

```bash
scripts/render-diagrams.sh path/to/plantuml.jar
```

## Technology

Java 21 (Gradle multi-module: `backend`, `frontend:cli`, `frontend:gui`), Swing desktop GUI, `study` CLI, local SQLite with a local embedding index, and an LLM accessed through the provider-independent `LLMClient` interface (Google Gemini by default, or any OpenAI-compatible endpoint).

## Running the application

### Prerequisites

- JDK 21 (`java -version` should print 21 or later). Gradle is not needed: the wrapper `./gradlew` downloads it.
- Optional but recommended: a free Gemini API key from <https://aistudio.google.com/apikey>. Without a key, the deterministic parts still work (courses, upload and retrieval, grading of objective questions, scheduling, progress, reminders, history) and AI features use their fallbacks or report that the AI is unavailable.

### Local configuration (API key)

```bash
cp .env.example .env
# edit .env and replace YOUR_LOCAL_API_KEY_HERE with your own key
```

`.env` is listed in `.gitignore` and must never be committed. To use an OpenAI-compatible provider instead, set `LLM_PROVIDER=openai` and the `OPENAI_*` values. Data is stored in `~/.study-agent/study-agent.db` (change it with `STUDY_AGENT_DB`).

### Build and test

```bash
./gradlew build          # compiles all modules and runs the JUnit tests
```

### GUI

```bash
./gradlew :frontend:gui:run
```

Create a course in **Courses**, upload notes in **Materials** (PDF, DOCX, TXT or Markdown), then use the other tabs: Tutor, Study Plan, Quiz, Flashcards, Learning Session, Progress, Concept Map, Reminders and History. The window shows "AI: connected" when a key is configured.

### CLI

```bash
./gradlew :frontend:cli:installDist
alias study="$PWD/frontend/cli/build/install/study/bin/study"

study help
study course create --code EECS3311 --title "Software Design" --exam 2026-12-15
study material upload --course EECS3311 notes/week5.pdf
study material list --course EECS3311
study material summarize <materialId> --length brief
study material extract-concepts <materialId>
study ask --course EECS3311 "What is the difference between the Adapter and Facade patterns?"
study study-plan generate --course EECS3311 --minutes 60
study quiz generate --course EECS3311 --count 5 --types mcq,tf --take
study learning-session start --course EECS3311
study progress show --course EECS3311
study history show --last 7d
```

Run `study` with no arguments for an interactive shell; add `--trace` to any AI command to see the agent's plan and tool results. The GUI and CLI share the same database, so work done in one appears in the other.
