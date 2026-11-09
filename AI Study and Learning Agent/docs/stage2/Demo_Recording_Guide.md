# Demonstration video script (Stage 2)

The Stage 2 instructions require, for **every feature**: the feature ID and name, how it is invoked and its input, the feature running and its output, where AI is involved, and an error or alternative case. Together the videos must show **both the GUI and the CLI**. Never show `.env` or your API key.

Plan: 17 short videos (N01 + F01-F16), 1-2 minutes each, recorded in the order below because later features use the data created by earlier ones. Name each file after its feature, e.g. `F05 Question Answering.mov`.

---

## Part 1 - Prepare (once, about 10 minutes)

1. **Close other apps and notifications** (Focus mode on), and make the screen tidy.
2. **Open a terminal** in VS Code (Terminal > New Terminal) and run these lines one by one. They use a fresh demo database so your videos start empty, and build the CLI:

   ```bash
   cd "/Users/nazaninsoleimani/Documents/EECS 3311/AI Study and Learning Agent"
   export STUDY_AGENT_DB="$PWD/build/demo/video.db"
   rm -f "$STUDY_AGENT_DB"
   ./gradlew -q :frontend:gui:installDist :frontend:cli:installDist
   alias study="$PWD/frontend/cli/build/install/study/bin/study"
   frontend/gui/build/install/gui/bin/gui &
   ```

   The **sign-in screen** opens (purple). Don't create the account yet: you will do it on camera in the first video (N01).
3. **Make the terminal text large** (Cmd + several times) so the CLI is readable in the video.
4. **Prepare one "wrong" file** for the error case in F02:

   ```bash
   echo "slides" > ~/Desktop/lecture-slides.pptx
   ```

5. **Recording tool**: press **Cmd + Shift + 5**, choose "Record Selected Portion" (or entire screen), click **Options > Microphone > MacBook Microphone**, then **Record**. Stop with the stop button in the menu bar. The video is saved to your Desktop.
6. Do a **10-second test recording** and play it back to check the sound.

Tips: speak slowly; it is fine to pause while the AI is "thinking" (say "the agent is retrieving my notes and generating..."). If a step fails because the AI provider is busy, wait a moment and click again; you can keep it in the video and explain that the app retries automatically.

**AI provider:** the app uses OpenAI. Before recording, check that `.env` has `LLM_PROVIDER=openai` and your `OPENAI_API_KEY=...` (each on its own line), and that the header badge says **AI connected**.

---

## Part 2 - The videos

Each video: **say the feature ID and name first**, then do the steps, then the error case, then one sentence about AI.

### N01 - Student Accounts (new feature) (GUI + CLI)

**Do:** On the sign-in screen click **New here? Create an account**. Name `Nazanin`, Email (any address you like, e.g. `nazanin@example.com`), Password with at least 8 characters, type it twice. Click **Create account**: the main window opens with your name at the top right. Check the badge says **AI connected** (green). Click **Sign out**: you are back on the sign-in screen.
**Error case:** sign in with a wrong password: "Email or password is incorrect". Then sign in with the right one.
**CLI:** in the terminal run `study account login --email nazanin@example.com`, type the password (it is not shown while typing), then `study account whoami`.

**Say:** "N01, Student Accounts. This is a new feature added in Stage 2. Each student has an account, so courses, progress and history are private. Passwords are never stored, only a salted hash. The same account works in the GUI and in the command line. This feature is deterministic."


### F01 - Course Management (GUI + CLI)

**Do:** Sidebar **Courses**. Code `EECS3311`, Title `Software Design`, Exam: a date about 3 weeks from today (format `2026-11-30`). Click **Create course**. Then edit the title to `Software Design and Patterns`, click **Save changes**.
**Error case:** Click **Create course** again with the same code: "You already have a course with code EECS3311". Then type an exam date in the past (`2025-01-01`) and click **Create course**: "Exam date ... is in the past".
**CLI:** in the terminal, run `study course list`.

**Say:** "F01, Course Management. A course is the workspace for all my materials, quizzes and plans. I create it with a code, title and exam date. The system rejects a duplicate code and an exam date in the past. The same courses appear in the command-line interface. This feature is deterministic; no AI is involved."

### F02 - Learning Material Upload (GUI + CLI)

**Do:** Sidebar **Materials**. Click **Upload file...**, open `sample-data/EECS3311` in the project folder, select all three `.md` files, **Open**. The files appear in the table within seconds: status **INDEXED** and the number of chunks. The panel below shows a green "Uploaded ..." card and "Extracting key concepts in the background...".
**Error case (CLI):** `study material upload --course EECS3311 ~/Desktop/lecture-slides.pptx` shows "Unsupported file type; use PDF, DOCX, TXT or Markdown".
**CLI:** `study material list --course EECS3311`.

**Say:** "F02, Material Upload. I upload my own lecture notes. The system extracts the text, splits it into chunks and indexes them for search. Unsupported files are rejected. The embedding step is deterministic. Right after indexing, the AI extracts the concepts in the background, which is F04, so I can keep working while it runs."

### F04 - Concept Extraction (GUI + CLI)

**Do:** In **Materials**, when the background extraction finishes the panel under the table shows the **KEY CONCEPTS** page: one card per concept with its definition and a **Quiz me** link. Select `week07-factory-method-adapter-facade.md` and click **Extract concepts** to re-run it. Then click **See how these concepts connect on the Concept Map** to show them on the map.
**CLI:** `study material list --course EECS3311`, copy one material id, then `study material extract-concepts <id>`.

**Say:** "F04, Concept Extraction. This is AI-based: the language model reads my notes and returns the key concepts and their relations as JSON. The agent validates the output, merges duplicates and drops relations to unknown concepts. Concepts are the unit the system uses to track my mastery."

### F03 - Document Summarization (GUI)

**Do:** In **Materials**, select `week05-observer-pattern.md`, choose Summary length **BRIEF**, click **Summarize**. Point at the header (reading time, number of key points and sources), the numbered **Key points to remember** and the **Where this comes from** sources. Then choose **DETAILED** and summarize again: the summary is split into numbered topic cards, with code and terms highlighted. At the bottom, **Ready to lock this in?** links straight to a quiz, flashcards, the tutor and the concept map.
**Alternative case:** say that long documents are summarised section by section and then combined (map-reduce), and if the AI is busy it shows "Summary unavailable, retry later" instead of a partial summary.

**Say:** "F03, Summarization. AI-based. The agent's planner checks the size of the material: short notes are summarised in one pass, long ones section by section and then combined. The output is validated before it is shown and stored."

### F05 - Question Answering (GUI + CLI)

**Do:** Sidebar **Tutor**. Tick **Show agent plan**. Ask: `What is the difference between the Strategy and State patterns?` Point at the green **Grounded in your notes** badge, the purple citation tags [1], the **Sources** chips and the **AGENT PLAN** box below the answer. Then ask the follow-up `Why is that useful?`.
**Error case:** Ask `What is photosynthesis?`: the answer card turns amber with **Not found in your material**, and the tutor says the course material does not cover this (it does not invent an answer).
**CLI:** `study ask --course EECS3311 "What does the Facade pattern do?"`

**Say:** "F05, Question Answering. Hybrid. The agent plans the steps: it retrieves the most relevant chunks of my notes, then the AI answers only from them and must cite them. A validator checks the citations and regenerates the answer if they are missing. Follow-up questions use the conversation memory. For an unrelated question nothing is found, so the AI is not even called."

### F08 - Quiz Generation (GUI)

**Do:** Sidebar **Quiz**. Questions **5**, Difficulty **INTERMEDIATE**, keep all three types ticked, Concepts empty. Click **Generate quiz**. Scroll through the question cards: each shows its number, type, difficulty and concept (multiple choice, true/false, short answer).
**Error case:** untick all three types and click **Generate quiz**: "Select at least one question type". Tick them again.

**Say:** "F08, Quiz Generation. AI-based. The AI writes question specifications from my notes; each one is turned into a question object by a factory for its type, the Factory Method pattern, which rejects invalid questions, for example a multiple-choice question whose answer is not one of the options."

### F09 - Automatic Quiz Grading (GUI + CLI)

**Do:** Answer the questions of the quiz from F08 (click anywhere on an option row): get some right and **some wrong on purpose**, and leave one question empty. Point at the "N of 5 answered" bar next to **Submit answers**. Click **Submit answers**: a dialog warns that one question is unanswered; click **Yes**. Show the score card at the top, the correct option in green ("Correct answer"), your wrong choice in red ("Your answer"), and the result banner under each question.
**CLI:** `study quiz list --course EECS3311`, then `study quiz take <quizId>` and answer in the terminal (type `true`, `A`, or a sentence).

**Say:** "F09, Automatic Grading. Hybrid. Multiple-choice and true/false are graded exactly by the question objects; short answers are graded by the AI against the model answer and rubric. Unanswered questions score zero. The results update my concept mastery."

### F10 - Answer Explanation (GUI + CLI)

**Do:** On the graded quiz, click **Explain why** in the red banner of a question you got wrong. Read part of the explanation and its sources.
**Error case (CLI):** `study quiz explain <attemptId> 9` shows "Question number must be 1-5" (the attempt id is printed at the end of `quiz take`).

**Say:** "F10, Answer Explanation. AI-based. The agent retrieves the relevant part of my notes and the AI explains why the correct answer is correct and what my mistake suggests, citing the source."

### F13 - Learning Progress Tracking (GUI + CLI)

**Do:** Sidebar **Progress**: the table of concepts with mastery bars. Then sidebar **Dashboard**: the Mastery card. Go to **Quiz**, generate a short quiz (3 questions), submit it, then return to **Progress** and point at the line "Updated: ... % to ... %" (the view updated itself).
**CLI:** `study progress show --course EECS3311`.

**Say:** "F13, Progress Tracking. Deterministic. Every graded answer updates the concept's mastery as a moving average. The progress view is an observer of the progress tracker, the Observer pattern, so it refreshes automatically after any quiz, flashcard review or practice session."

### F11 - Weak-Topic Identification (GUI + CLI)

**Do:** In **Progress**, click **Analyze weak topics**. Each weak concept is a card: **#1 MOST URGENT**, its mastery bar, **WHAT IS GOING WRONG** (the AI's diagnosis) and a purple button for the recommended activity. Click that button to show it opens the right screen (for example **Start a learning session**).
**Alternative case:** first show it on a new empty course, or explain the message "Not enough graded practice yet: ... of the 5 graded answers needed".
**CLI:** `study weak-topics show --course EECS3311`.

**Say:** "F11, Weak Topics. Hybrid. The ranking is deterministic: a concept is weak when its mastery is below 50 percent after at least three attempts, and priority combines weakness, time since practice and prerequisites. With enough data, the AI reads my wrong answers and describes the likely misconception."

### F12 - Adaptive Question Difficulty (CLI + GUI)

**Do (CLI):** `study learning-session start --course EECS3311`. Answer **three questions correctly in a row**: the terminal prints ">> Difficulty up: FOUNDATION -> INTERMEDIATE". Then answer **two wrong in a row**: "Difficulty down". Type `q` to finish and show the summary with the level history.
**GUI:** sidebar **Learning Session**, click **Start session**, answer one question, show the log.

**Say:** "F12, Adaptive Difficulty. Hybrid. Each level is a state object, the State pattern: three correct answers move me up, two wrong move me down, and the state decides the question types. The AI generates each question for my weakest concept at the current level. If the AI is unavailable, questions from earlier quizzes are reused."

### F06 - Personalized Study Plan (GUI + CLI)

**Do:** Sidebar **Study Plan**. Minutes per day **60**, Focus topics `State Pattern`, click **Generate plan**. Point at the overview card: the strategy tag, days to the exam, the rationale and **Today**'s tasks. Tick the **Done** box of the first task: the % complete goes up. Select a **Quiz** task and click **Start selected task**: the quiz opens on that concept. Then go to **Dashboard**: today's tasks and the plan progress card.
**Alternative case:** explain that the strategy depends on the time left: under a week is "Exam cram", over three weeks "Spaced repetition", otherwise "Balanced review". If you have time, change the course exam date to 5 days from now and generate again to show "Exam cram".
**CLI:** `study study-plan show --course EECS3311`.

**Say:** "F06, Study Plan. Hybrid. The agent analyses my mastery, retrieves my notes, and the AI prioritises topics and writes an objective for each one. The schedule is built by a scheduling strategy chosen from the days left before the exam, the Strategy pattern. When my mastery drops, the plan is revised automatically."

### F07 - Flashcards (CLI + GUI)

**Do (CLI):** `study flashcards generate --course EECS3311 --topic "observer pattern" --count 5`. Then `study flashcards review --course EECS3311`: press Enter to reveal, type `3` (good) or `1` (again); show the next review date.
**GUI:** sidebar **Flashcards**: point at "Card 1 of N" and the memory level dots. Click the card (or press **Space**): it flips to the green **ANSWER** side. Rate it with **Good** or **Again** (or keys **3** / **1**). After the last card, "All caught up!" appears.
**Error case:** in the GUI, clear the Topic and click **Generate deck**: "Enter a topic for the cards".

**Say:** "F07, Flashcards. Hybrid. The AI writes the cards from my notes; empty or duplicate cards are dropped. Reviews are deterministic, Leitner boxes: a good answer moves the card to a longer interval, a wrong one resets it to tomorrow."

### F14 - Study Reminders (GUI + CLI)

**Do:** Sidebar **Reminders**. Set When to **2 minutes from now** (format `2026-11-09 18:42`), Repeat **Once**, Message `Review the Observer pattern`, **Add reminder**. Add a second one with the quick pick **Tomorrow 18:00** and Repeat **Every day**. Point at the summary line ("2 upcoming, next in 2 min") and the status tags. Select one and click **Cancel selected**. Wait for the desktop notification of the first one (or click **Send due reminders now** after the time): its tag changes to **Sent** (tick **Show sent and cancelled**).
**Error case:** set When to yesterday and click **Add reminder**: "Reminder time is in the past".
**CLI:** `study reminder list`.

**Say:** "F14, Reminders. Deterministic. Reminders are delivered as desktop notifications, daily and weekly ones repeat. The agent also creates reminders for my plan, and the reminder scheduler observes my progress to remind me when a concept becomes weak."

(If no notification appears: System Settings > Notifications > allow notifications for "Script Editor".)

### F15 - Concept Relationship Visualization (GUI + CLI)

**Do:** Sidebar **Concept Map**: explain the layout (prerequisites on the left, the concepts they lead to on the right), the line styles in the legend (prerequisite, part of, related) and the colours (grey not practised, red weak, amber learning, green mastered). Hover over a concept: its connections are highlighted. Click it: the panel on the right shows its definition, mastery and related concepts. Click **Quiz me on this**: the app opens the Quiz with that concept.
**CLI:** `study concept-map show --course EECS3311` and `study concept-map show --course EECS3311 --format mermaid`.
**Alternative case:** create a second course in **Courses** and open the map: "No concepts yet".

**Say:** "F15, Concept Map. Hybrid: the relations were extracted by the AI in F04; drawing the map and colouring it by my mastery is deterministic."

### F16 - Learning Session History (GUI + CLI)

**Do:** Sidebar **History**: the summary cards (activities, quizzes taken, cards reviewed, tutor questions, tasks done) and the timeline grouped by day, newest first. Change **Activity** to **Quiz completed**, change **Period**.
**Error case:** set Period **Today** and Activity **Reminder sent** (or anything unused): "0 event(s)" and "Nothing here yet".
**CLI:** `study history show --course EECS3311 --last 7d --type quiz_completed`.

**Say:** "F16, History. Deterministic. Every action is recorded; the agent also reads my recent history as context when it plans and answers."

---

## Part 3 - Upload and send the links

1. Upload the 17 videos into **one folder in your personal Google Drive** (not your York account).
2. Right-click the folder > **Share** > General access: **Anyone with the link**, role **Viewer** > **Copy link**.
3. For each video: right-click > **Share** > **Copy link**.
4. Open each link in a **private/incognito browser window** to check it plays without logging in (Drive needs a few minutes to process videos).
5. Send the links in this form, and they will be put in the report (sections 2 and 7), the PDF rebuilt and pushed:

```
Folder: https://drive.google.com/...
F01 https://drive.google.com/...
F02 https://drive.google.com/...
...
F16 https://drive.google.com/...
N01 https://drive.google.com/...
```
