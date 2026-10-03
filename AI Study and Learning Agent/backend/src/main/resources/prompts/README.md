# Prompt templates and output schemas

Planned contents (Stage 2): one prompt template and one JSON output schema per `PromptTask` used by `ContextBuilder` and `ResponseValidator`, for example:

| Template | Used by | Output schema |
|---|---|---|
| `answer-with-citations` | F05 Q&A (SD02) | answer text + cited source numbers |
| `prioritize-topics` | F06 study plan (SD01) | topic priorities, objectives, rationale |
| `summarize-section`, `summarize-reduce` | F03 summarization (SD05) | summary, key points, page refs |
| `extract-concepts` | F04 concept extraction (SD07) | concepts + typed relations |
| `generate-quiz`, `adaptive-activity` | F08, F12 (SD03, SD04) | list of `QuestionSpec` |
| `generate-flashcards` | F07 (SD08) | front/back pairs |
| `grade-short-answer` | F09 (SD03) | score + feedback |
| `explain-answer` | F10 (SD03) | explanation + source refs |
| `diagnose-weakness` | F11 (SD06) | misconception + recommended activity |

Keeping prompts outside Java code lets them be reviewed and versioned separately from the logic that validates their output.
