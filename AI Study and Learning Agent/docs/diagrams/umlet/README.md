# UML Diagrams (UMLet)

These are the official Stage 1 UML diagrams, drawn in **UMLet 15.1** (`.uxf` files), as the course requires.
Every diagram is fully editable: classes, lifelines, messages, frames and notes are normal UMLet palette elements.

## Files

| File | Diagram | Report section |
|---|---|---|
| `00-architecture.uxf` | Layered component architecture | 3 |
| `01-use-case.uxf` | Use-case diagram (UC01-UC17) | 6 |
| `02-class-diagram.uxf` | Complete specification-level class diagram | 4 |
| `02a-class-presentation-application.uxf` | View A - Presentation and Application layers (Facade, Observer) | 4 |
| `02b-class-agent-tools.uxf` | View B - Agent layer and agent tools (Strategy, Factory Method, State) | 4 |
| `02c-class-domain.uxf` | View C - Domain model | 4 |
| `02d-class-integration-persistence.uxf` | View D - AI/Integration and Persistence layers (Adapter) | 4 |
| `03-sequence-study-plan.uxf` | SD01 - Personalized study plan generation | 7 |
| `04-sequence-material-qa.uxf` | SD02 - Material Q&A | 7 |
| `05-sequence-quiz.uxf` | SD03 - Quiz generation and grading | 7 |
| `06-sequence-adaptive-learning.uxf` | SD04 - Adaptive learning session | 7 |
| `07-sequence-summarization.uxf` | SD05 - Material summarization | 7 |
| `08-sequence-progress-analysis.uxf` | SD06 - Progress analysis | 7 |
| `09-sequence-material-upload.uxf` | SD07 - Material upload | 7 |
| `10-sequence-flashcards-cli.uxf` | SD08 - Flashcards (CLI) | 7 |
| `11-sequence-reminders.uxf` | SD09 - Study reminders | 7 |
| `12-sequence-concept-map.uxf` | SD10 - Concept map | 7 |
| `13-sequence-course-history-cli.uxf` | SD11 - Course history (CLI) | 7 |
| `14-deployment.uxf` | Deployment diagram (planned) | 3.9 |

`png/` holds a preview image of each diagram so they can be viewed on GitHub without UMLet.

## Opening and exporting

1. Download UMLet from https://www.umlet.com (standalone) or install the UMLet extension for VS Code / Eclipse.
2. Open any `.uxf` file (File > Open).
3. To export: File > Export as... > PNG, PDF or SVG.

To export all diagrams at once:

```bash
scripts/export-umlet.sh path/to/umlet.jar png   # or pdf / svg
```

## Notation notes

- Sequence diagrams: filled arrowheads are synchronous calls, dashed open arrows are returns, `<<create>>` messages are dashed and point at the new object's head. Messages are numbered in order. `alt`, `loop`, `opt` and `group` frames are UMLet frame elements; dashed lines inside an `alt` separate its operands.
- Class diagrams: hollow triangle = generalization, dashed hollow triangle = interface realization, filled diamond = composition, hollow diamond = aggregation, dashed open arrow = dependency.
- The PlantUML sources one folder up (`../*.puml`) describe the same model and are kept for reference only.
