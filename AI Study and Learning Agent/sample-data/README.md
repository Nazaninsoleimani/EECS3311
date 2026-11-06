# Sample data

Safe example course material for trying the application and recording the demonstrations. The notes were written for this project (no copyrighted lecture slides) and contain no personal data.

| File | Topics | Good for trying |
|---|---|---|
| `EECS3311/week05-observer-pattern.md` | Observer: participants, push vs pull, consequences | Q&A with citations, summaries, concept extraction |
| `EECS3311/week06-strategy-and-state.md` | Strategy, State, how they differ | quizzes, flashcards, "Strategy versus State" questions |
| `EECS3311/week07-factory-method-adapter-facade.md` | Factory Method, Adapter, Facade | concept map relations, weak-topic analysis after a quiz |

## Quick start

GUI: create a course (for example code `EECS3311`), open **Materials**, click **Upload file...** and select the three files.

CLI:

```bash
study course create --code EECS3311 --title "Software Design" --exam 2026-12-15
study material upload --course EECS3311 sample-data/EECS3311/week05-observer-pattern.md
study material upload --course EECS3311 sample-data/EECS3311/week06-strategy-and-state.md
study material upload --course EECS3311 sample-data/EECS3311/week07-factory-method-adapter-facade.md
study ask --course EECS3311 "What is the difference between Strategy and State?"
```

`scripts/demo-cli.sh` runs a complete CLI walkthrough over these files in a separate demo database.
