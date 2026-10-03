# AI Study and Learning Agent

**EECS 3311 Software Design, Fall 2026 - Course Project**
Nazanin Fatemeh Soleimani (222050579)
Repository: <https://github.com/Nazaninsoleimani/EECS3311.git>

A personalized AI learning agent for university students. It works over the student's own course materials to answer questions with source citations, summarize lectures, extract concepts, build and revise study plans, generate and grade quizzes and flashcards, identify weak topics, adapt practice difficulty, and track progress. All functionality is available through both a GUI and a CLI that share the same backend facade.

## Project status

| Stage | Content | Status |
|---|---|---|
| **Stage 1 - Design** | Project definition, 16 features, use-case model (17 use cases), class model, 6 design patterns, 11 sequence diagrams, deployment diagram, traceability | Complete: [`docs/stage1/Stage1_Report.md`](docs/stage1/Stage1_Report.md) ([PDF](docs/stage1/Stage1_Report.pdf)) |
| Stage 2 | Implementation | Planned (`backend/`, `frontend/`, `docs/stage2/`) |
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

Stage 1 is the design stage, so the code folders contain README files that list the classes from the class model to be implemented there. No implementation code is included yet.

## Diagrams

The official UML diagrams are the UMLet files in [`docs/diagrams/umlet/`](docs/diagrams/umlet/). Open them in UMLet 15.1 or export them all with:

```bash
scripts/export-umlet.sh path/to/umlet.jar png
```

The PlantUML sources describe the same model. All class diagrams are generated from one shared model (`docs/diagrams/class-model.iuml`), so the views always stay consistent.

```bash
scripts/render-diagrams.sh path/to/plantuml.jar
```

## Planned technology (Stage 2, subject to change)

Java (Gradle multi-module), desktop GUI, `study` CLI, local SQLite with a local embedding index, and an LLM accessed through the provider-independent `LLMClient` interface (Google Gemini primary, OpenAI alternative).
