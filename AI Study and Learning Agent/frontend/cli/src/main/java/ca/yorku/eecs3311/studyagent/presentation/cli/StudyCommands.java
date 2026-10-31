package ca.yorku.eecs3311.studyagent.presentation.cli;

import ca.yorku.eecs3311.studyagent.agent.AgentException;
import ca.yorku.eecs3311.studyagent.application.ActivityOutcome;
import ca.yorku.eecs3311.studyagent.application.ConceptMapService;
import ca.yorku.eecs3311.studyagent.application.LearningFacade;
import ca.yorku.eecs3311.studyagent.domain.AdaptiveLearningSession;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.DifficultyLevel;
import ca.yorku.eecs3311.studyagent.domain.EventType;
import ca.yorku.eecs3311.studyagent.domain.Explanation;
import ca.yorku.eecs3311.studyagent.domain.Flashcard;
import ca.yorku.eecs3311.studyagent.domain.FlashcardDeck;
import ca.yorku.eecs3311.studyagent.domain.FlashcardRequest;
import ca.yorku.eecs3311.studyagent.domain.GradeResult;
import ca.yorku.eecs3311.studyagent.domain.HistoryFilter;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import ca.yorku.eecs3311.studyagent.domain.LearningProgress;
import ca.yorku.eecs3311.studyagent.domain.MultipleChoiceQuestion;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.QuestionType;
import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.domain.QuizAttempt;
import ca.yorku.eecs3311.studyagent.domain.QuizRequest;
import ca.yorku.eecs3311.studyagent.domain.Recurrence;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReminderStatus;
import ca.yorku.eecs3311.studyagent.domain.ReviewRating;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.domain.WeakTopic;
import ca.yorku.eecs3311.studyagent.domain.WeakTopicReport;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** CLI commands for F06-F16: planning, practice, progress, reminders, concept map and history. */
class StudyCommands {
    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, QuestionType> TYPE_ALIASES = Map.of("mcq", QuestionType.MULTIPLE_CHOICE,
            "tf", QuestionType.TRUE_FALSE, "short", QuestionType.SHORT_ANSWER);

    private final CLIApplication cli;
    private final LearningFacade facade;
    private final String me;

    StudyCommands(CLIApplication cli) {
        this.cli = cli;
        this.facade = cli.facade;
        this.me = cli.studentId;
    }

    void registerAll() {
        cli.register("study-plan generate", "study study-plan generate --course <c> --minutes <n> [--exam yyyy-mm-dd] [--focus a,b]",
                "F06: AI study plan up to the exam (strategy depends on the time left)", cmd -> {
                    Course c = cli.course(cmd);
                    LocalDate exam = cmd.options.containsKey("exam") ? cmd.dateOption("exam") : c.getExamDate();
                    StudyPlan plan = facade.generateStudyPlan(new StudyPlanRequest(me, c.getId(), exam,
                            cmd.intOption("minutes", 60), cmd.listOption("focus")));
                    return formatPlan(plan, 7);
                });
        cli.register("study-plan show", "study study-plan show --course <c> [--all]", "F06: show the active plan (next 7 days, or --all)", cmd -> {
            StudyPlan plan = facade.getActivePlan(me, cli.course(cmd).getId());
            return plan == null ? "No active study plan. Create one with 'study study-plan generate'." : formatPlan(plan, cmd.has("all") ? 10_000 : 7);
        });
        cli.register("study-plan done", "study study-plan done <taskId>", "F06: mark a study task as completed", cmd -> {
            StudyTask t = facade.completeStudyTask(cmd.arg(0, "task id"));
            return "Done: " + t.getObjective();
        });

        cli.register("flashcards generate", "study flashcards generate --course <c> --topic <t> [--count n] [--concepts a,b]",
                "F07: AI flashcards from your material", cmd -> {
                    FlashcardDeck d = facade.generateFlashcards(new FlashcardRequest(me, cli.course(cmd).getId(),
                            cmd.listOption("concepts"), cmd.option("topic", null), cmd.intOption("count", 10)));
                    StringBuilder sb = new StringBuilder(d.getTitle() + " (" + d.getCards().size() + " cards, deck " + d.getId() + ")\n");
                    d.getCards().forEach(card -> sb.append("  Q: ").append(card.getFront()).append("\n  A: ").append(card.getBack()).append("\n\n"));
                    if (d.getNotice() != null) sb.append(d.getNotice());
                    return sb.toString().trim();
                });
        cli.register("flashcards list", "study flashcards list --course <c>", "F07: list decks and due cards", cmd -> {
            String courseId = cli.course(cmd).getId();
            List<FlashcardDeck> decks = facade.listDecks(courseId);
            if (decks.isEmpty()) return "No flashcard decks yet.";
            LocalDate today = LocalDate.now();
            return decks.stream().map(d -> String.format("%-12s %-40s %d cards, %d due", d.getId(), d.getTitle(),
                    d.getCards().size(), d.dueCards(today).size())).collect(Collectors.joining("\n"));
        });
        cli.register("flashcards review", "study flashcards review --course <c>",
                "F07: review due cards (Leitner boxes); rate again/hard/good/easy", cmd -> reviewFlashcards(cli.course(cmd)));

        cli.register("quiz generate", "study quiz generate --course <c> [--concepts a,b] [--count n] [--types mcq,tf,short] [--difficulty foundation|intermediate|advanced] [--take]",
                "F08: AI quiz validated question by question", cmd -> {
                    List<QuestionType> types = cmd.listOption("types").stream().map(StudyCommands::questionType).toList();
                    Quiz quiz = facade.generateQuiz(new QuizRequest(me, cli.course(cmd).getId(), cmd.listOption("concepts"),
                            cmd.intOption("count", 5), types, cmd.enumOption("difficulty", DifficultyLevel.class, null)));
                    String header = "Quiz " + quiz.getId() + ": " + quiz.getQuestions().size() + " " + quiz.getDifficulty().name().toLowerCase()
                            + " question(s)." + (quiz.getNotice() == null ? "" : "\n" + quiz.getNotice());
                    if (cmd.has("take")) {
                        cli.print(header);
                        return takeQuiz(quiz);
                    }
                    return header + "\nTake it with: study quiz take " + quiz.getId();
                });
        cli.register("quiz list", "study quiz list --course <c>", "F08: list generated quizzes", cmd -> {
            List<Quiz> quizzes = facade.listQuizzes(cli.course(cmd).getId());
            if (quizzes.isEmpty()) return "No quizzes yet.";
            return quizzes.stream().map(q -> String.format("%-12s %s  %d question(s), %s", q.getId(),
                    q.getCreatedAt().format(WHEN), q.getQuestions().size(), q.getDifficulty())).collect(Collectors.joining("\n"));
        });
        cli.register("quiz take", "study quiz take <quizId>", "F09: answer a quiz interactively; graded at the end",
                cmd -> takeQuiz(facade.findQuiz(cmd.arg(0, "quiz id"))));
        cli.register("quiz explain", "study quiz explain <attemptId> <questionNumber|questionId>",
                "F10: AI explanation of a graded answer, backed by your material", cmd -> {
                    QuizAttempt attempt = facade.findAttempt(cmd.arg(0, "attempt id"));
                    String q = cmd.arg(1, "question number or id");
                    if (q.matches("\\d+")) {
                        List<Question> questions = facade.findQuiz(attempt.getQuizId()).getQuestions();
                        int n = Integer.parseInt(q);
                        if (n < 1 || n > questions.size()) throw new ValidationException("Question number must be 1-" + questions.size());
                        q = questions.get(n - 1).getId();
                    }
                    Explanation e = facade.explainAnswer(attempt.getId(), q);
                    return e.text + (e.sourceRefs.isEmpty() ? "" : "\n\nSources:\n  " + String.join("\n  ", e.sourceRefs));
                });

        cli.register("weak-topics show", "study weak-topics show --course <c>", "F11: weakest concepts with diagnosis and next activity",
                cmd -> formatWeakTopics(facade.analyzeWeakTopics(me, cli.course(cmd).getId())));
        cli.register("learning-session start", "study learning-session start --course <c>",
                "F12: adaptive practice; difficulty rises after 3 correct and falls after 2 wrong", cmd -> learningSession(cli.course(cmd)));
        cli.register("progress show", "study progress show --course <c>", "F13: mastery per concept, quiz average and plan completion",
                cmd -> formatProgress(facade.getProgress(me, cli.course(cmd).getId())));

        cli.register("reminder add", "study reminder add --at \"yyyy-mm-dd hh:mm\" [--repeat daily|weekly] [--course <c>] \"<message>\"",
                "F14: schedule a study reminder", cmd -> {
                    String courseId = cmd.options.containsKey("course") ? cli.course(cmd).getId() : null;
                    Reminder r = facade.scheduleReminder(new Reminder(Ids.newId("r"), me, courseId, cmd.text("message"),
                            dateTime(cmd.require("at")), cmd.enumOption("repeat", Recurrence.class, Recurrence.NONE)));
                    return "Reminder " + r.getId() + " set for " + r.getDueAt().format(WHEN)
                            + (r.getRecurrence() == Recurrence.NONE ? "" : ", repeating " + r.getRecurrence().name().toLowerCase());
                });
        cli.register("reminder list", "study reminder list [--all]", "F14: pending reminders (--all includes sent and cancelled)", cmd -> {
            List<Reminder> list = facade.listReminders(me).stream()
                    .filter(r -> cmd.has("all") || r.getStatus() == ReminderStatus.PENDING).toList();
            if (list.isEmpty()) return "No reminders.";
            return list.stream().map(r -> String.format("%-12s %s  %-9s %-7s %s", r.getId(), r.getDueAt().format(WHEN),
                    r.getStatus(), r.getRecurrence(), r.getMessage())).collect(Collectors.joining("\n"));
        });
        cli.register("reminder cancel", "study reminder cancel <reminderId>", "F14: cancel a pending reminder", cmd -> {
            facade.cancelReminder(cmd.arg(0, "reminder id"));
            return "Cancelled.";
        });
        cli.register("reminder dispatch", "study reminder dispatch", "F14: deliver reminders that are due now",
                cmd -> facade.dispatchDueReminders() + " reminder(s) delivered.");

        cli.register("concept-map show", "study concept-map show --course <c> [--format text|mermaid]",
                "F15: concepts, relations and your mastery of each", cmd -> {
                    ConceptGraph g = facade.getConceptMap(me, cli.course(cmd).getId());
                    return "mermaid".equalsIgnoreCase(cmd.option("format", "text")) && !g.isEmpty() ? g.toMermaid() : ConceptMapService.renderText(g);
                });
        cli.register("history show", "study history show [--course <c>] [--last 7d|2w] [--type quiz_completed,...]",
                "F16: your learning history, newest first", cmd -> {
                    String courseId = cmd.options.containsKey("course") ? cli.course(cmd).getId() : null;
                    List<EventType> types = cmd.listOption("type").stream()
                            .map(t -> ParsedCommand.parseEnum(t, EventType.class, "--type")).toList();
                    List<LearningEvent> events = facade.getHistory(me, new HistoryFilter(courseId, since(cmd.option("last", null)), null, types));
                    if (events.isEmpty()) return "No matching history.";
                    return events.stream().map(e -> String.format("%s  %-20s %s", e.getTimestamp().format(WHEN), e.getType(), e.getSummary()))
                            .collect(Collectors.joining("\n"));
                });
    }

    // ------------------------------------------------------------------ interactive flows

    private String takeQuiz(Quiz quiz) {
        List<QuestionResponse> responses = new ArrayList<>();
        List<Question> questions = quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            cli.print("\n" + formatQuestion(i + 1, q));
            String a = cli.prompt("Your answer: ");
            if (a == null) break;
            responses.add(new QuestionResponse(q.getId(), a.trim()));
        }
        QuizAttempt attempt = facade.submitQuiz(quiz.getId(), responses);
        StringBuilder sb = new StringBuilder(String.format("%nScore: %.0f%% (%d of %d correct)%n", attempt.computeScore() * 100,
                attempt.correctCount(), attempt.getResults().size()));
        for (int i = 0; i < questions.size(); i++) {
            GradeResult r = attempt.findResult(questions.get(i).getId()).orElseThrow();
            sb.append(String.format("  Q%d %s %s%n", i + 1, r.correct ? "correct  " : "incorrect", r.feedback));
        }
        sb.append("Attempt ").append(attempt.getId()).append(". Explain an answer with: study quiz explain ")
          .append(attempt.getId()).append(" <question number>");
        return sb.toString();
    }

    private String reviewFlashcards(Course c) {
        List<Flashcard> due = facade.dueFlashcards(c.getId());
        if (due.isEmpty()) return "No cards due today.";
        cli.print(due.size() + " card(s) due. Press Enter to reveal, then rate 1=again 2=hard 3=good 4=easy (q to stop).");
        int reviewed = 0;
        for (Flashcard card : due) {
            cli.print("\nQ: " + card.getFront());
            String reveal = cli.prompt("(Enter to show answer) ");
            if (reveal == null || reveal.trim().equalsIgnoreCase("q")) break;
            cli.print("A: " + card.getBack());
            ReviewRating rating = null;
            while (rating == null) {
                String in = cli.prompt("Rating [1-4]: ");
                if (in == null || in.trim().equalsIgnoreCase("q")) return "Reviewed " + reviewed + " card(s).";
                rating = rating(in);
                if (rating == null) cli.print("Type 1, 2, 3, 4 or again/hard/good/easy.");
            }
            Flashcard updated = facade.reviewFlashcard(card.getId(), rating);
            cli.print("Next review: " + updated.getNextReview() + " (box " + updated.getBox() + ")");
            reviewed++;
        }
        return "Reviewed " + reviewed + " card(s).";
    }

    private String learningSession(Course c) {
        AdaptiveLearningSession s = facade.startAdaptiveSession(me, c.getId());
        cli.print("Adaptive session for " + c.getCode() + " at " + s.getLevel() + " level. Type q to finish.");
        Question q = s.getCurrentQuestion();
        int n = 1;
        while (q != null) {
            cli.print("\n" + formatQuestion(n++, q));
            String a = cli.prompt("Your answer: ");
            if (a == null || a.trim().equalsIgnoreCase("q")) break;
            ActivityOutcome out;
            try {
                out = facade.submitActivityResult(s.getId(), new QuestionResponse(q.getId(), a.trim()));
            } catch (AgentException | LLMException e) {
                cli.print("Error: " + e.getMessage());
                break;
            }
            cli.print((out.result().correct ? "Correct. " : "Not quite. ") + out.result().feedback);
            if (out.levelChanged()) cli.print(">> Difficulty " + (out.level().ordinal() > out.previousLevel().ordinal() ? "up" : "down")
                    + ": " + out.previousLevel() + " -> " + out.level());
            if (out.notice() != null) cli.print(out.notice());
            q = out.next();
        }
        AdaptiveLearningSession ended = facade.endAdaptiveSession(s.getId());
        return String.format("Session finished: %d of %d correct. Levels: %s", ended.getCorrect(), ended.getAnswered(),
                String.join(" -> ", ended.getLevelHistory()));
    }

    // ------------------------------------------------------------------ formatting and parsing

    static String formatQuestion(int n, Question q) {
        StringBuilder sb = new StringBuilder(String.format("Q%d [%s, %s%s] %s", n, q.getType().name().toLowerCase().replace('_', ' '),
                q.getDifficulty().name().toLowerCase(), q.getConceptName() == null ? "" : ", " + q.getConceptName(), q.getPrompt()));
        if (q instanceof MultipleChoiceQuestion mcq) {
            for (int i = 0; i < mcq.getOptions().size(); i++) sb.append("\n   ").append((char) ('A' + i)).append(") ").append(mcq.getOptions().get(i));
        } else if (q.getType() == QuestionType.TRUE_FALSE) {
            sb.append("\n   (true / false)");
        }
        return sb.toString();
    }

    static String formatPlan(StudyPlan plan, int days) {
        StringBuilder sb = new StringBuilder(String.format("%s plan (id %s, revision %d), %.0f%% complete%n%s%n",
                plan.getStrategyName(), plan.getId(), plan.getRevision(), plan.completionRate() * 100, plan.getRationale()));
        LocalDate first = plan.getTasks().stream().map(StudyTask::getDate).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate shownUntil = first.plusDays(days);
        LocalDate current = null;
        for (StudyTask t : plan.getTasks()) {
            if (!t.getDate().isBefore(shownUntil)) break;
            if (!t.getDate().equals(current)) {
                current = t.getDate();
                sb.append('\n').append(current).append(" (").append(current.getDayOfWeek().name().substring(0, 3)).append(")\n");
            }
            sb.append(String.format("  [%s] %-12s %3d min  %-16s %s%n", t.isDone() ? "x" : " ", t.getId(), t.getDurationMinutes(),
                    t.getActivity(), t.getObjective()));
        }
        long hidden = plan.getTasks().stream().filter(t -> !t.getDate().isBefore(shownUntil)).count();
        if (hidden > 0) sb.append("\n... ").append(hidden).append(" more task(s); use --all to see the whole plan.\n");
        plan.getWarnings().forEach(w -> sb.append("Note: ").append(w).append('\n'));
        return sb.toString().trim();
    }

    static String formatWeakTopics(WeakTopicReport r) {
        StringBuilder sb = new StringBuilder(r.message == null ? "" : r.message + "\n");
        for (WeakTopic t : r.topics) {
            sb.append(String.format("%n- %s  (mastery %.0f%%, priority %.2f)%n    %s%n    Recommended: %s%n", t.conceptName,
                    t.mastery * 100, t.priority, t.diagnosis, t.recommendedActivity));
        }
        if (r.aiDiagnosed) sb.append("\nDiagnoses written by the AI from your wrong answers.");
        return sb.toString().trim();
    }

    static String formatProgress(LearningProgress p) {
        if (p.isEmpty()) return "No graded activity yet. Take a quiz, review flashcards or start a learning session.";
        StringBuilder sb = new StringBuilder(String.format("Average mastery %.0f%%, quiz average %.0f%%, plan %.0f%% complete, "
                        + "%d concept(s) mastered, %d graded activities%n%n", p.averageMastery * 100, p.quizAverage * 100,
                p.planCompletion * 100, p.conceptsMastered, p.gradedActivities));
        p.masteries.stream().sorted((a, b) -> Double.compare(a.getMastery(), b.getMastery())).forEach(m -> sb.append(String.format(
                "  %-30s %s %3.0f%%  (%d attempts)%s%n", m.getConceptName(), bar(m.getMastery()), m.getMastery() * 100, m.getAttempts(),
                m.isWeak() ? "  weak" : m.isMastered() ? "  mastered" : "")));
        return sb.toString().trim();
    }

    static String bar(double value) {
        int filled = (int) Math.round(value * 20);
        return "[" + "#".repeat(filled) + ".".repeat(20 - filled) + "]";
    }

    static QuestionType questionType(String s) {
        QuestionType alias = TYPE_ALIASES.get(s.toLowerCase(Locale.ROOT));
        return alias != null ? alias : ParsedCommand.parseEnum(s, QuestionType.class, "--types");
    }

    static ReviewRating rating(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "1", "again" -> ReviewRating.AGAIN;
            case "2", "hard" -> ReviewRating.HARD;
            case "3", "good" -> ReviewRating.GOOD;
            case "4", "easy" -> ReviewRating.EASY;
            default -> null;
        };
    }

    static LocalDateTime dateTime(String s) {
        try {
            return LocalDateTime.parse(s.trim().replace('T', ' '), WHEN);
        } catch (DateTimeParseException e) {
            throw new ValidationException("--at must look like \"2026-11-05 18:30\", got '" + s + "'");
        }
    }

    /** "7d" or "2w" before today; null means no lower bound. */
    static LocalDate since(String last) {
        if (last == null) return null;
        String t = last.trim().toLowerCase(Locale.ROOT);
        if (!t.matches("\\d+[dw]")) throw new ValidationException("--last must look like 7d or 2w, got '" + last + "'");
        int n = Integer.parseInt(t.substring(0, t.length() - 1));
        return LocalDate.now().minusDays(t.endsWith("w") ? 7L * n : n);
    }
}
