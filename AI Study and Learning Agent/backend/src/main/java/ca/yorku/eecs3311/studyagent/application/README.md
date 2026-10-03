# Package `ca.yorku.eecs3311.studyagent.application`

Application layer: the single entry point for both front ends (`LearningFacade`, Facade pattern), input validation, deterministic services, and progress notification (Observer pattern).

> Stage 1 is the design stage, so this folder contains no implementation yet. The classes listed below come from the Stage 1 class model (`docs/diagrams/class-model.iuml`) and will be implemented here in Stage 2.

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

Realized features: all (F01-F16 enter through `LearningFacade`).
