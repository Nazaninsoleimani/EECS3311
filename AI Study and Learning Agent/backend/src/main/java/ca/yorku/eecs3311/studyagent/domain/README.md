# Package `ca.yorku.eecs3311.studyagent.domain`

Domain model: learning entities and the deterministic rules closest to their data (mastery update, Leitner scheduling, polymorphic grading, difficulty state context). No dependencies on other layers.

> Stage 1 is the design stage, so this folder contains no implementation yet. The classes listed below come from the Stage 1 class model (`docs/diagrams/class-model.iuml`) and will be implemented here in Stage 2.

| Class | Kind | Notes |
|---|---|---|
| `Student` | class |  |
| `StudentProfile` | class |  |
| `Course` | class |  |
| `CourseMaterial` | class |  |
| `DocumentChunk` | class |  |
| `Summary` | class |  |
| `Concept` | class |  |
| `ConceptRelation` | class |  |
| `ConceptGraph` | class |  |
| `StudyPlanRequest` | class |  |
| `StudyPlan` | class |  |
| `StudyTask` | class |  |
| `TopicPriority` | class |  |
| `Quiz` | class |  |
| `Question` | abstract class |  |
| `MultipleChoiceQuestion` | class |  |
| `TrueFalseQuestion` | class |  |
| `ShortAnswerQuestion` | class |  |
| `QuizAttempt` | class |  |
| `QuestionResponse` | class |  |
| `GradeResult` | class |  |
| `Explanation` | class |  |
| `Answer` | class |  |
| `FlashcardDeck` | class |  |
| `Flashcard` | class |  |
| `ConceptMastery` | class |  |
| `PerformanceRecord` | class |  |
| `LearningProgress` | class |  |
| `WeakTopicReport` | class |  |
| `WeakTopic` | class |  |
| `LearningSession` | class |  |
| `AdaptiveLearningSession` | class | «Context» |
| `LearningHistory` | class |  |
| `LearningEvent` | class |  |
| `Reminder` | class |  |
| `ProgressEvent` | class |  |
| `QuizRequest` | class |  |
| `FlashcardRequest` | class |  |
| `QuestionSpec` | class |  |
| `SummaryOptions` | class |  |
| `HistoryFilter` | class |  |
| `DifficultyLevel` | enum |  |
| `QuestionType` | enum |  |
