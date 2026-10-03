# Deployment

Planned deployment for Stage 2 and Stage 3. The UML deployment diagram is `docs/diagrams/umlet/14-deployment.uxf` (UMLet; exported: `docs/stage1/img/14-deployment.png`).

## Target

A single-user desktop application on the student's own computer:

- Java runtime running three artifacts: `study-agent-core.jar` (backend), `study-agent-gui.jar`, `study-agent-cli.jar`.
- Local SQLite database file holding courses, materials, chunks, embeddings, progress and history.
- External services over HTTPS: Google Gemini API (primary LLM and embeddings), OpenAI API (alternative LLM).
- Operating-system notification centre for reminders.

## Configuration

- `config/application.example.properties`: copy to `application.properties` and adjust.
- `.env.example`: copy to `.env` and add API keys. Never commit real keys (`.env` is in `.gitignore`).

## Planned packaging (Stage 2/3, subject to change)

- Build with Gradle (multi-module: backend, frontend/gui, frontend/cli).
- Desktop installers with `jpackage`; the CLI is also distributed as a runnable jar with a `study` launcher script.
- Data stays on the student's machine; only text needed for a request is sent to the configured AI provider.
