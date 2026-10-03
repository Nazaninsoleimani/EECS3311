# Tests (Stage 2)

Planned test strategy:

- **Unit tests (no LLM):** mastery update (`ConceptMastery.applyResult`), Leitner rule (`Flashcard.recordReview`), polymorphic grading (`Question.grade`), difficulty transitions (each `DifficultyState`), scheduling strategies, factory validation (`QuestionFactory.build`), retrieval threshold.
- **Agent flow tests:** every `LearningAgent` goal run against a deterministic fake `LLMClient`, checking the `AgentPlan`, tool calls, fallbacks and error paths shown in SD01 to SD11.
- **Contract tests:** each repository interface tested against the SQLite implementation.
- **Adapter tests:** `GeminiLLMAdapter` / `OpenAILLMAdapter` error and response mapping with recorded responses.
