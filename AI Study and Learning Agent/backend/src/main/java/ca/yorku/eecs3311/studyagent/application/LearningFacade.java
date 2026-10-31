package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.agent.AgentException;
import ca.yorku.eecs3311.studyagent.agent.LearningAgent;
import ca.yorku.eecs3311.studyagent.agent.MemoryManager;
import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.AdaptiveLearningSession;
import ca.yorku.eecs3311.studyagent.domain.Answer;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.DifficultyLevel;
import ca.yorku.eecs3311.studyagent.domain.DifficultyState;
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
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.domain.QuizAttempt;
import ca.yorku.eecs3311.studyagent.domain.QuizRequest;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReviewRating;
import ca.yorku.eecs3311.studyagent.domain.Student;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.SummaryOptions;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.domain.WeakTopicReport;
import ca.yorku.eecs3311.studyagent.integration.llm.UsageMeter;
import ca.yorku.eecs3311.studyagent.persistence.FlashcardRepository;
import ca.yorku.eecs3311.studyagent.persistence.QuizRepository;
import ca.yorku.eecs3311.studyagent.persistence.StudentRepository;
import ca.yorku.eecs3311.studyagent.persistence.StudyPlanRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade pattern: the single entry point used by both the GUI and the CLI. It coordinates the
 * application services, the agent and the repositories so that neither front end depends on
 * the subsystems behind it, and records every action in the learning history (F16).
 */
public class LearningFacade {
    private final Map<String, AdaptiveLearningSession> activeSessions = new ConcurrentHashMap<>();
    private final CourseService courseService;
    private final MaterialService materialService;
    private final GradingService grading;
    private final ProgressTracker tracker;
    private final ReminderScheduler reminders;
    private final ConceptMapService conceptMaps;
    private final StudyPlanMonitor planMonitor;
    private final LearningAgent agent;
    private final MemoryManager memory;
    private final StudyPlanRepository plans;
    private final QuizRepository quizzes;
    private final FlashcardRepository flashcards;
    private final StudentRepository students;
    private final UsageMeter usage;

    public LearningFacade(CourseService courseService, MaterialService materialService, GradingService grading,
                          ProgressTracker tracker, ReminderScheduler reminders, ConceptMapService conceptMaps,
                          StudyPlanMonitor planMonitor, LearningAgent agent, MemoryManager memory, StudyPlanRepository plans,
                          QuizRepository quizzes, FlashcardRepository flashcards, StudentRepository students, UsageMeter usage) {
        this.courseService = courseService;
        this.materialService = materialService;
        this.grading = grading;
        this.tracker = tracker;
        this.reminders = reminders;
        this.conceptMaps = conceptMaps;
        this.planMonitor = planMonitor;
        this.agent = agent;
        this.memory = memory;
        this.plans = plans;
        this.quizzes = quizzes;
        this.flashcards = flashcards;
        this.students = students;
        this.usage = usage;
    }

    // ------------------------------------------------------------------ F01 courses

    public Course createCourse(String studentId, String code, String title, LocalDate examDate) {
        Course c = courseService.createCourse(studentId, code, title, examDate);
        record(studentId, c.getId(), EventType.COURSE_CREATED, "Created course " + c, c.getId());
        return c;
    }

    public List<Course> listCourses(String studentId) {
        return courseService.listCourses(studentId);
    }

    public Course updateCourse(Course course) {
        Course c = courseService.updateCourse(course);
        record(c.getStudentId(), c.getId(), EventType.COURSE_UPDATED, "Updated course " + c, c.getId());
        return c;
    }

    public void deleteCourse(String courseId) {
        Course c = courseService.find(courseId);
        courseService.deleteCourse(courseId);
        record(c.getStudentId(), null, EventType.COURSE_DELETED, "Deleted course " + c.getCode(), courseId);
    }

    public Course resolveCourse(String studentId, String idOrCode) {
        return courseService.resolve(studentId, idOrCode);
    }

    // ------------------------------------------------------------------ F02-F04 materials

    public CourseMaterial uploadMaterial(String courseId, Path file) {
        Course c = courseService.find(courseId);
        CourseMaterial m = materialService.upload(courseId, file);
        record(c.getStudentId(), courseId, EventType.MATERIAL_UPLOADED, "Uploaded " + m.getTitle() + " (" + m.getStatus()
                + (m.isIndexed() ? ", " + m.getChunkCount() + " chunks" : ": " + m.getFailureReason()) + ")", m.getId());
        return m;
    }

    public List<CourseMaterial> listMaterials(String courseId) {
        return materialService.list(courseId);
    }

    public void deleteMaterial(String materialId) {
        CourseMaterial m = materialService.find(materialId);
        materialService.delete(materialId);
        record(studentOf(m.getCourseId()), m.getCourseId(), EventType.MATERIAL_DELETED, "Deleted " + m.getTitle(), materialId);
    }

    public Summary summarizeMaterial(String materialId, SummaryOptions options) {
        CourseMaterial m = indexedMaterial(materialId);
        String studentId = studentOf(m.getCourseId());
        Summary s = agent.summarize(studentId, m, options);
        materialService.saveSummary(materialId, s);
        record(studentId, m.getCourseId(), EventType.MATERIAL_SUMMARIZED, s.getLength() + " summary of " + m.getTitle(), materialId);
        return s;
    }

    public ConceptGraph extractConcepts(String materialId) {
        CourseMaterial m = indexedMaterial(materialId);
        String studentId = studentOf(m.getCourseId());
        ConceptGraph stored = conceptMaps.save(m.getCourseId(), agent.extractConcepts(studentId, m));
        record(studentId, m.getCourseId(), EventType.CONCEPTS_EXTRACTED, stored.nodes.size() + " concept(s) from " + m.getTitle(), materialId);
        return stored;
    }

    private CourseMaterial indexedMaterial(String materialId) {
        CourseMaterial m = materialService.find(materialId);
        if (!m.isIndexed()) throw new ValidationException("'" + m.getTitle() + "' was not indexed: " + m.getFailureReason());
        return m;
    }

    // ------------------------------------------------------------------ F05 tutor

    public Answer askQuestion(String sessionId, String courseId, String question) {
        String studentId = studentOf(courseId);
        Answer a = agent.answerQuestion(studentId, sessionId, courseId, question);
        record(studentId, courseId, EventType.QUESTION_ASKED, question + (a.grounded ? "" : " (not grounded)"), sessionId);
        return a;
    }

    public String newSessionId() {
        return Ids.newId("chat");
    }

    // ------------------------------------------------------------------ F06 study plan

    public StudyPlan generateStudyPlan(StudyPlanRequest request) {
        StudyPlan plan = agent.generateStudyPlan(request);
        plans.findActive(request.studentId, request.courseId).ifPresent(old -> {
            old.archive();
            plans.save(old);
        });
        plans.save(plan);
        record(request.studentId, request.courseId, EventType.PLAN_GENERATED,
                plan.getStrategyName() + " plan with " + plan.getTasks().size() + " task(s)", plan.getId());
        return plan;
    }

    public StudyPlan getActivePlan(String studentId, String courseId) {
        return plans.findActive(studentId, courseId).orElse(null);
    }

    public StudyTask completeStudyTask(String taskId) {
        StudyPlan plan = plans.findByTask(taskId).orElseThrow(() -> new ValidationException("No study task with id " + taskId));
        StudyTask task = plan.findTask(taskId).orElseThrow();
        task.complete();
        plans.save(plan);
        record(plan.getStudentId(), plan.getCourseId(), EventType.TASK_COMPLETED, task.getObjective(), taskId);
        return task;
    }

    public void setPlanRevisionListener(java.util.function.Consumer<StudyPlan> listener) {
        planMonitor.setOnRevised(listener);
    }

    // ------------------------------------------------------------------ F07 flashcards

    public FlashcardDeck generateFlashcards(FlashcardRequest request) {
        FlashcardDeck deck = agent.generateFlashcards(request);
        flashcards.saveDeck(deck);
        record(request.studentId, request.courseId, EventType.FLASHCARDS_GENERATED,
                deck.getCards().size() + " card(s): " + deck.getTitle(), deck.getId());
        return deck;
    }

    public List<FlashcardDeck> listDecks(String courseId) {
        return flashcards.findDecksByCourse(courseId);
    }

    public List<Flashcard> dueFlashcards(String courseId) {
        LocalDate today = LocalDate.now();
        List<Flashcard> due = new ArrayList<>();
        for (FlashcardDeck d : flashcards.findDecksByCourse(courseId)) due.addAll(d.dueCards(today));
        return due;
    }

    /** Leitner-box update; the rating also counts as a graded result for the card's concept. */
    public Flashcard reviewFlashcard(String cardId, ReviewRating rating) {
        FlashcardDeck deck = flashcards.findDeckByCard(cardId).orElseThrow(() -> new ValidationException("No flashcard with id " + cardId));
        Flashcard card = deck.findCard(cardId).orElseThrow();
        card.recordReview(rating);
        flashcards.saveDeck(deck);
        String studentId = studentOf(deck.getCourseId());
        tracker.recordResult(studentId, deck.getCourseId(), card.toGradeResult(rating), card.getFront(), rating.name(),
                DifficultyLevel.FOUNDATION, ActivityType.FLASHCARDS);
        record(studentId, deck.getCourseId(), EventType.FLASHCARD_REVIEWED, rating + ": " + card.getFront(), cardId);
        return card;
    }

    // ------------------------------------------------------------------ F08-F10 quizzes

    public Quiz generateQuiz(QuizRequest request) {
        Quiz quiz = agent.generateQuiz(request);
        quizzes.save(quiz);
        record(request.studentId, request.courseId, EventType.QUIZ_GENERATED,
                quiz.getQuestions().size() + " " + quiz.getDifficulty() + " question(s)", quiz.getId());
        return quiz;
    }

    public Quiz findQuiz(String quizId) {
        return quizzes.findById(quizId).orElseThrow(() -> new ValidationException("No quiz with id " + quizId));
    }

    public List<Quiz> listQuizzes(String courseId) {
        return quizzes.findByCourse(courseId);
    }

    public QuizAttempt submitQuiz(String quizId, List<QuestionResponse> responses) {
        Quiz quiz = findQuiz(quizId);
        String studentId = studentOf(quiz.getCourseId());
        QuizAttempt attempt = grading.grade(quiz, studentId, responses);
        quizzes.saveAttempt(attempt);
        tracker.recordAttempt(attempt, quiz);
        record(studentId, quiz.getCourseId(), EventType.QUIZ_COMPLETED, String.format("Scored %.0f%% (%d/%d)",
                attempt.computeScore() * 100, attempt.correctCount(), attempt.getResults().size()), attempt.getId());
        return attempt;
    }

    public QuizAttempt findAttempt(String attemptId) {
        return quizzes.findAttempt(attemptId).orElseThrow(() -> new ValidationException("No quiz attempt with id " + attemptId));
    }

    public Explanation explainAnswer(String attemptId, String questionId) {
        QuizAttempt attempt = findAttempt(attemptId);
        Quiz quiz = findQuiz(attempt.getQuizId());
        Question q = quiz.findQuestion(questionId).orElseThrow(() -> new ValidationException("No question " + questionId + " in this quiz"));
        Explanation e = agent.explainAnswer(attempt.getStudentId(), quiz.getCourseId(), q,
                attempt.findResponse(questionId).orElse(null), attempt.findResult(questionId).orElse(null));
        record(attempt.getStudentId(), quiz.getCourseId(), EventType.ANSWER_EXPLAINED, q.getPrompt(), attemptId);
        return e;
    }

    // ------------------------------------------------------------------ F11 weak topics, F13 progress

    public WeakTopicReport analyzeWeakTopics(String studentId, String courseId) {
        WeakTopicReport r = agent.diagnoseWeakTopics(studentId, courseId);
        record(studentId, courseId, EventType.WEAK_TOPICS_ANALYZED, r.topics.size() + " topic(s)"
                + (r.aiDiagnosed ? ", AI diagnosis" : ""), null);
        return r;
    }

    public LearningProgress getProgress(String studentId, String courseId) {
        return tracker.getProgress(studentId, courseId);
    }

    public void attachProgressObserver(ProgressObserver o) {
        tracker.attach(o);
    }

    public void detachProgressObserver(ProgressObserver o) {
        tracker.detach(o);
    }

    // ------------------------------------------------------------------ F12 adaptive learning

    public AdaptiveLearningSession startAdaptiveSession(String studentId, String courseId) {
        DifficultyLevel start = students.findById(studentId).map(s -> s.getProfile().getPreferredDifficulty()).orElse(DifficultyLevel.FOUNDATION);
        AdaptiveLearningSession s = new AdaptiveLearningSession(Ids.newId("as"), studentId, courseId, DifficultyState.initial(start));
        agent.nextAdaptiveActivity(s);
        activeSessions.put(s.getId(), s);
        record(studentId, courseId, EventType.SESSION_STARTED, "Adaptive session at " + s.getLevel(), s.getId());
        return s;
    }

    /** Grades the answer, lets the session's DifficultyState react, records progress, then asks for the next question. */
    public ActivityOutcome submitActivityResult(String sessionId, QuestionResponse response) {
        AdaptiveLearningSession s = session(sessionId);
        Question q = s.getCurrentQuestion();
        if (q == null) throw new ValidationException("This session has no open question");
        GradeResult result = grading.gradeResponse(q, response);
        DifficultyLevel before = s.getLevel();
        s.recordOutcome(result);
        tracker.recordActivity(s.getStudentId(), s.getCourseId(), q, response, result);
        Question next = null;
        String notice = null;
        try {
            next = agent.nextAdaptiveActivity(s);
        } catch (AgentException e) {
            s.setCurrentQuestion(null);
            notice = "Could not generate the next question: " + e.getMessage();
        }
        return new ActivityOutcome(result, before, s.getLevel(), next, notice);
    }

    public AdaptiveLearningSession endAdaptiveSession(String sessionId) {
        AdaptiveLearningSession s = session(sessionId);
        s.end();
        activeSessions.remove(sessionId);
        record(s.getStudentId(), s.getCourseId(), EventType.SESSION_ENDED, String.format("%d/%d correct, levels %s",
                s.getCorrect(), s.getAnswered(), String.join(" -> ", s.getLevelHistory())), sessionId);
        return s;
    }

    private AdaptiveLearningSession session(String sessionId) {
        AdaptiveLearningSession s = activeSessions.get(sessionId);
        if (s == null) throw new ValidationException("No active learning session " + sessionId);
        return s;
    }

    // ------------------------------------------------------------------ F14 reminders

    public Reminder scheduleReminder(Reminder reminder) {
        return reminders.schedule(reminder);
    }

    public void cancelReminder(String reminderId) {
        reminders.cancel(reminderId);
    }

    public List<Reminder> listReminders(String studentId) {
        return reminders.list(studentId);
    }

    public int dispatchDueReminders() {
        return reminders.dispatchDue(LocalDateTime.now());
    }

    public void startReminderTimer(int intervalSeconds) {
        reminders.start(intervalSeconds);
    }

    public void stopReminderTimer() {
        reminders.stop();
    }

    // ------------------------------------------------------------------ F15 concept map, F16 history

    public ConceptGraph getConceptMap(String studentId, String courseId) {
        return conceptMaps.buildGraph(studentId, courseId);
    }

    public List<LearningEvent> getHistory(String studentId, HistoryFilter filter) {
        return memory.getHistory(studentId, filter);
    }

    // ------------------------------------------------------------------ status

    public Student getStudent(String studentId) {
        return students.findById(studentId).orElseThrow(() -> new ValidationException("No student " + studentId));
    }

    public String status() {
        return "LLM: " + (agent.isLlmAvailable() ? agent.getModelName() : "not configured (deterministic fallbacks in use)")
                + "\nLLM calls this run: " + usage.getCalls() + ", tokens: " + usage.getTokens();
    }

    public boolean isLlmAvailable() { return agent.isLlmAvailable(); }

    public String getLastAgentTrace() { return agent.getLastTrace(); }

    private String studentOf(String courseId) {
        return courseService.find(courseId).getStudentId();
    }

    private void record(String studentId, String courseId, EventType type, String summary, String refId) {
        memory.recordEvent(new LearningEvent(studentId, courseId, type, summary, refId));
    }
}
