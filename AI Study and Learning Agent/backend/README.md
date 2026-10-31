# Backend (core module)

The backend is the core of the AI Study and Learning Agent. It is a library module (planned artifact `study-agent-core.jar`) used by both front ends in `frontend/gui` and `frontend/cli`. Both front ends talk to it only through `LearningFacade`, so no business logic is duplicated.

This is a single-user desktop application, so "backend" and "frontend" are modules running in the same process (see the deployment diagram, `docs/stage1/img/14-deployment.png`), not a client and a web server.

> Implemented in Stage 2. The table lists the classes of the Stage 1 class model (`docs/diagrams/class-model.iuml`) that live here; the Java sources are in this folder.

## Package layout (one package per architectural layer)

| Folder | Layer (report section) | May depend on |
|---|---|---|
| `src/main/java/ca/yorku/eecs3311/studyagent/application` | Application layer (3.3) | agent, domain, integration, persistence |
| `src/main/java/ca/yorku/eecs3311/studyagent/agent` | Agent layer (3.4) | domain, integration, persistence |
| `src/main/java/ca/yorku/eecs3311/studyagent/agent/tools` | Agent tools (4.4) | domain, integration, persistence |
| `src/main/java/ca/yorku/eecs3311/studyagent/domain` | Domain layer (3.5) | nothing outside domain |
| `src/main/java/ca/yorku/eecs3311/studyagent/integration` | AI / Integration layer (3.6) | domain |
| `src/main/java/ca/yorku/eecs3311/studyagent/persistence` | Persistence layer (3.7) | domain |
| `src/main/resources/prompts` | Prompt templates and JSON output schemas | - |
| `src/test/java/ca/yorku/eecs3311/studyagent` | Unit and integration tests | all |

Dependencies point downward only. The only upward call (application to presentation) goes through the `ProgressObserver` interface (Observer pattern, report 5.4).
