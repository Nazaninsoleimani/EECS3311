# Package `ca.yorku.eecs3311.studyagent.application`

Application layer: the single entry point for both front ends (`LearningFacade`, Facade pattern), input validation, deterministic services, and progress notification (Observer pattern).

> Implemented in Stage 2. The table lists the classes of the Stage 1 class model (`docs/diagrams/class-model.iuml`) that live here; the Java sources are in this folder.

| Class | Kind | Notes |
|---|---|---|
| `LearningFacade` | class | «Facade» |
| `CourseService` | class |  |
| `MaterialService` | class |  |
| `GradingService` | class |  |
| `ProgressTracker` | class | «Subject» |
| `ProgressObserver` | interface | «interface» «Observer» |
| `StudyPlanMonitor` | class |  |
| `ReminderScheduler` | class |  |
| `ConceptMapService` | class |  |
| `ActivityOutcome` | record | added in Stage 2: result of one adaptive answer (grade, level change, next question) |

Realized features: all (F01-F16 enter through `LearningFacade`).
