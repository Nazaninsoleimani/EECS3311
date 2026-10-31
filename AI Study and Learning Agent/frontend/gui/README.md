# GUI front end (package `ca.yorku.eecs3311.studyagent.presentation.gui`)

Desktop course dashboard. Every view extends `BaseView` and calls only `LearningFacade`.

> Implemented in Stage 2. The table lists the classes of the Stage 1 class model (`docs/diagrams/class-model.iuml`) that live here; the Java sources are in this folder.

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
| `StudyAgentGui` | class | added in Stage 2: main window and entry point |
| `GuiContext` | class | added in Stage 2: selected course and refresh events shared by the views |
| `QuestionPanel` | class | added in Stage 2: question display and answer input shared by `QuizView` and `LearningSessionView` |

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
