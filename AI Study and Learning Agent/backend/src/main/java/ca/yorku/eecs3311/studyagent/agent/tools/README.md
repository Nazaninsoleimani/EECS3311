# Package `ca.yorku.eecs3311.studyagent.agent.tools`

The tools the agent can invoke, plus the Strategy (study scheduling), Factory Method (question creation) and State (adaptive difficulty) participants.

> Implemented in Stage 2. The table lists the classes of the Stage 1 class model (`docs/diagrams/class-model.iuml`) that live here; the Java sources are in this folder.

| Class | Kind | Notes |
|---|---|---|
| `Tool` | interface | «interface» |
| `ToolInput` | class |  |
| `ToolResult` | class |  |
| `RetrievalTool` | class |  |
| `SummarizationTool` | class |  |
| `ConceptExtractionTool` | class |  |
| `QuizGenerationTool` | class |  |
| `FlashcardGenerationTool` | class |  |
| `ProgressAnalysisTool` | class |  |
| `StudyScheduleTool` | class |  |
| `ReminderTool` | class |  |
| `StudyPlanningStrategy` | interface | «interface» «Strategy» |
| `ExamCramStrategy` | class |  |
| `SpacedRepetitionStrategy` | class |  |
| `BalancedReviewStrategy` | class |  |
| `QuestionFactory` | abstract class | «Creator» |
| `MultipleChoiceQuestionFactory` | class |  |
| `TrueFalseQuestionFactory` | class |  |
| `ShortAnswerQuestionFactory` | class |  |
| `DifficultyState` | interface | «interface» «State» |
| `FoundationState` | class |  |
| `IntermediateState` | class |  |
| `AdvancedState` | class |  |

Rule: every AI tool's output passes deterministic validation (`ResponseValidator`, `QuestionFactory.build()`) before it becomes a domain object.
