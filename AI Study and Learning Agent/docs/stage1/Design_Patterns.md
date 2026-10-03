# Design Patterns

*AI Study and Learning Agent - EECS 3311 Fall 2026 Stage 1. Extracted from [Stage1_Report.md](Stage1_Report.md); diagrams are in [`img/`](img/) and the UMLet sources in [`../diagrams/umlet/`](../diagrams/umlet/).*

## 5. Design Patterns

Six GoF patterns are used. Each solves a specific problem in this application; none was added only to reach the minimum of five.

### 5.1 Strategy - study scheduling

| | |
|---|---|
| **Problem** | How study time is distributed depends on the situation: a student with 4 days before an exam needs intensive, weak-first practice; a student with 6 weeks benefits from spaced repetition; a student with no exam date needs balanced review. Encoding all three in one method would produce a large conditional that grows with every new approach. |
| **Participants and roles** | *Strategy*: `StudyPlanningStrategy` (`buildSchedule()`). *Concrete strategies*: `ExamCramStrategy`, `SpacedRepetitionStrategy`, `BalancedReviewStrategy`. *Context*: `StudyScheduleTool`, which holds the current strategy and selects it in `selectStrategy(request)` (≤ 7 days → cram; long horizon → spaced repetition; otherwise balanced). |
| **Rationale** | The algorithms share one input/output contract (request + priorities → tasks) but differ completely internally; they are interchangeable at runtime based on the request. |
| **Consequences** | New strategies (e.g. "interleaved practice") are added as one class with no change to the agent or the tool's callers; each strategy is unit-testable with fixed priorities. Without Strategy, `StudyScheduleTool` would contain all algorithms in one method, violating the open-closed principle and making each algorithm hard to test in isolation. |

### 5.2 Factory Method - question creation

| | |
|---|---|
| **Problem** | The LLM returns question specifications as JSON with a `type` field. The system must turn each into the correct `Question` subclass, validating type-specific constraints (MCQ needs 3-5 options and a valid `correctIndex`; true/false needs a boolean; short answer needs a model answer and rubric). Creating objects with `switch(type)` in the tool would couple it to every concrete question class. |
| **Participants and roles** | *Creator*: abstract `QuestionFactory` with the template operation `build(spec)` (common validation) and the factory method `createQuestion(spec)`. *Concrete creators*: `MultipleChoiceQuestionFactory`, `TrueFalseQuestionFactory`, `ShortAnswerQuestionFactory`. *Product*: abstract `Question`. *Concrete products*: `MultipleChoiceQuestion`, `TrueFalseQuestion`, `ShortAnswerQuestion`. *Client*: `QuizGenerationTool`, which holds one factory per `QuestionType`. |
| **Rationale** | Subclasses decide which product to instantiate and enforce product-specific validation; the client only knows `QuestionFactory` and `Question`. |
| **Consequences** | Adding a new question type (e.g. code-tracing questions) means one new product and one new factory registered in the map - `QuizGenerationTool`, `GradingService` (which grades through the polymorphic `grade()`) and the views do not change. Without it, every tool that creates questions (quiz generation and adaptive sessions) would repeat the conditional creation and validation logic. |

### 5.3 Facade - unified application API for GUI and CLI

| | |
|---|---|
| **Problem** | Each feature involves several subsystems (agent, services, repositories, retrieval). The GUI and the CLI must both access all features; if each called subsystems directly, the orchestration logic would be duplicated in two front-ends and the UI would be coupled to internal classes. |
| **Participants and roles** | *Facade*: `LearningFacade`. *Subsystem classes*: `LearningAgent`, `CourseService`, `MaterialService`, `GradingService`, `ProgressTracker`, `ReminderScheduler`, `ConceptMapService`, `MemoryManager`, repositories. *Clients*: all `BaseView` subclasses and `CLIApplication`. |
| **Rationale** | Directly satisfies the requirement that GUI and CLI expose the same functionality; it is the single place for input validation and persistence of agent results. |
| **Consequences** | Front-ends depend on one class; internal refactoring (e.g. splitting a service) does not affect them; a future web or test front-end can reuse it unchanged. Without it, `QuizView` and the CLI quiz command would each need to know about `GradingService`, `QuizRepository` and `ProgressTracker` and the call order between them. The trade-off - a large interface - is controlled by keeping the facade thin (delegation only). |

### 5.4 Observer - progress changes

| | |
|---|---|
| **Problem** | When mastery changes (after quizzes, adaptive activities or flashcard reviews), several independent parts must react: the progress dashboard refreshes, the study plan may need revision, and a review reminder may be needed. Hard-coding these calls into `ProgressTracker` would couple the tracker to the GUI, the agent and the reminder subsystem. |
| **Participants and roles** | *Subject*: `ProgressTracker` (`attach()`, `detach()`, `notifyObservers()`). *Observer*: `ProgressObserver` (`onProgressChanged(event)`). *Concrete observers*: `ProgressDashboardView`, `StudyPlanMonitor`, `ReminderScheduler`. *Event*: `ProgressEvent`. |
| **Rationale** | One-to-many dependency where the subject should not know concrete receivers; observers are attached at start-up (and the dashboard only while it is open). |
| **Consequences** | Adaptation (plan revision, reminders) happens automatically without the grading code knowing about it; new reactions (e.g. an achievements panel) are added as observers. Without Observer, `ProgressTracker.recordResult()` would call the view, the agent and the scheduler directly, creating an upward dependency from the application layer to the presentation layer. |

### 5.5 Adapter - LLM provider independence

| | |
|---|---|
| **Problem** | Gemini and OpenAI SDKs expose incompatible APIs (different method names, request structures, structured-output mechanisms and error types). The agent, tools and grading service must not depend on any vendor SDK. |
| **Participants and roles** | *Target*: `LLMClient` (`generate()`, `generateStructured()`, `getModelName()`). *Adapters*: `GeminiLLMAdapter`, `OpenAILLMAdapter`. *Adaptees*: `GeminiSdkClient` (`generateContent()`), `OpenAISdkClient` (`createChatCompletion()`). *Clients*: `LearningAgent`, `Planner`, generation tools, `GradingService`. |
| **Rationale** | The interface the system needs already exists (`LLMClient`); the vendor classes cannot be changed, so they are wrapped. Adapters also translate vendor-specific errors into one common error type and normalise token usage. |
| **Consequences** | Switching provider is a configuration change; tests can use a deterministic fake `LLMClient`. Without Adapter, vendor-specific code would appear in at least seven classes and a provider change (pricing, availability, course restrictions) would require editing all of them. |

### 5.6 State - adaptive difficulty

| | |
|---|---|
| **Problem** | An adaptive session behaves differently at each difficulty level: which question types are used, how hard they are, and when to move up or down. Implementing this with `if (level == ...)` checks inside `AdaptiveLearningSession` and `LearningAgent` would scatter the transition rules. |
| **Participants and roles** | *Context*: `AdaptiveLearningSession` (`recordOutcome()`, `setState()`, `getState()`). *State*: `DifficultyState` (`getLevel()`, `onResult()`, `preferredQuestionTypes()`). *Concrete states*: `FoundationState` (true/false and MCQ; up after 3 consecutive correct), `IntermediateState` (MCQ and short answer; up after 3 correct, down after 2 incorrect), `AdvancedState` (short answer and harder MCQ; down after 2 incorrect). |
| **Rationale** | Behaviour depends on a state that changes at runtime, and the transition rules belong naturally to each state. |
| **Consequences** | Transition rules are explicit, deterministic and unit-testable; adding a level (e.g. "Exam-ready") means adding one state class. Without State, difficulty logic would be duplicated between the session and the agent and would be hard to verify. This is also where the AI/deterministic split is clearest: the **state machine decides the difficulty**, the **LLM only generates content** at that difficulty. |

**Not counted as patterns.** The repository interfaces follow the Repository/DAO idea but are not GoF patterns; MVC describes the presentation architecture (§3.2); the `Tool` interface is a uniform capability interface used by the agent and is not presented as a separate pattern.
