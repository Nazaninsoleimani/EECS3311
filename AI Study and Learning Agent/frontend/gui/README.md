# GUI front end (planned package `ca.yorku.eecs3311.studyagent.presentation.gui`)

Desktop course dashboard. Every view extends `BaseView` and calls only `LearningFacade`.

> Stage 1 is the design stage, so this folder contains no implementation yet. The classes listed below come from the Stage 1 class model (`docs/diagrams/class-model.iuml`) and will be implemented here in Stage 2.

| Class | Kind | Notes |
|---|---|---|
| `BaseView` | abstract class |  |
| `DashboardView` | class |  |
| `CourseView` | class |  |
| `MaterialView` | class |  |
| `StudyPlannerView` | class |  |
| `TutorChatView` | class |  |
| `QuizView` | class |  |
| `FlashcardView` | class |  |
| `LearningSessionView` | class |  |
| `ProgressDashboardView` | class |  |
| `ConceptMapView` | class |  |
| `ReminderView` | class |  |
| `HistoryView` | class |  |

| View | Features |
|---|---|
| `DashboardView` | overview of F06, F13, F14 |
| `CourseView` | F01 |
| `MaterialView` | F02, F03, F04 |
| `TutorChatView` | F05 |
| `StudyPlannerView` | F06 |
| `FlashcardView` | F07 |
| `QuizView` | F08, F09, F10 |
| `ProgressDashboardView` | F11, F13 (also a `ProgressObserver`) |
| `LearningSessionView` | F12 |
| `ReminderView` | F14 |
| `ConceptMapView` | F15 |
| `HistoryView` | F16 |
