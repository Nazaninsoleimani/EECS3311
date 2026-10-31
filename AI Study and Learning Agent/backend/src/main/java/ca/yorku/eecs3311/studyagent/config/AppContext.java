package ca.yorku.eecs3311.studyagent.config;

import ca.yorku.eecs3311.studyagent.agent.ContextBuilder;
import ca.yorku.eecs3311.studyagent.agent.LearningAgent;
import ca.yorku.eecs3311.studyagent.agent.MemoryManager;
import ca.yorku.eecs3311.studyagent.agent.Planner;
import ca.yorku.eecs3311.studyagent.agent.ResponseValidator;
import ca.yorku.eecs3311.studyagent.agent.ToolRegistry;
import ca.yorku.eecs3311.studyagent.agent.tools.ConceptExtractionTool;
import ca.yorku.eecs3311.studyagent.agent.tools.FlashcardGenerationTool;
import ca.yorku.eecs3311.studyagent.agent.tools.ProgressAnalysisTool;
import ca.yorku.eecs3311.studyagent.agent.tools.QuizGenerationTool;
import ca.yorku.eecs3311.studyagent.agent.tools.ReminderTool;
import ca.yorku.eecs3311.studyagent.agent.tools.RetrievalTool;
import ca.yorku.eecs3311.studyagent.agent.tools.StudyScheduleTool;
import ca.yorku.eecs3311.studyagent.agent.tools.SummarizationTool;
import ca.yorku.eecs3311.studyagent.application.ConceptMapService;
import ca.yorku.eecs3311.studyagent.application.CourseService;
import ca.yorku.eecs3311.studyagent.application.GradingService;
import ca.yorku.eecs3311.studyagent.application.LearningFacade;
import ca.yorku.eecs3311.studyagent.application.MaterialService;
import ca.yorku.eecs3311.studyagent.application.ProgressTracker;
import ca.yorku.eecs3311.studyagent.application.ReminderScheduler;
import ca.yorku.eecs3311.studyagent.application.StudyPlanMonitor;
import ca.yorku.eecs3311.studyagent.domain.Student;
import ca.yorku.eecs3311.studyagent.integration.llm.GeminiSdkClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClientFactory;
import ca.yorku.eecs3311.studyagent.integration.llm.UsageMeter;
import ca.yorku.eecs3311.studyagent.integration.notification.NotificationGateway;
import ca.yorku.eecs3311.studyagent.integration.parsing.Chunker;
import ca.yorku.eecs3311.studyagent.integration.parsing.DocxParser;
import ca.yorku.eecs3311.studyagent.integration.parsing.PdfParser;
import ca.yorku.eecs3311.studyagent.integration.parsing.PlainTextParser;
import ca.yorku.eecs3311.studyagent.integration.retrieval.EmbeddingService;
import ca.yorku.eecs3311.studyagent.integration.retrieval.GeminiEmbeddingService;
import ca.yorku.eecs3311.studyagent.integration.retrieval.LocalHashEmbeddingService;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievalService;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteConceptRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteCourseRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteFlashcardRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteLearningHistoryRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteMaterialRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteProgressRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteQuizRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteReminderRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteStudentRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteStudyPlanRepository;
import ca.yorku.eecs3311.studyagent.integration.retrieval.SqliteVectorStore;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/**
 * Composition root: builds the object graph once (repositories, LLM adapter, agent and tools,
 * services, observers) and hands the front ends a ready LearningFacade. Both the GUI and the
 * CLI start here, so they always run the same backend.
 */
public final class AppContext implements AutoCloseable {
    private final SqliteDatabase db;
    private final LearningFacade facade;
    private final String studentId;
    private final AppConfig config;

    private AppContext(AppConfig config, SqliteDatabase db, LLMClient rawLlm, EmbeddingService embeddings, NotificationGateway gateway) {
        this.config = config;
        this.db = db;
        UsageMeter llm = new UsageMeter(rawLlm);

        var students = new SqliteStudentRepository(db);
        var courses = new SqliteCourseRepository(db);
        var materials = new SqliteMaterialRepository(db);
        var concepts = new SqliteConceptRepository(db);
        var progress = new SqliteProgressRepository(db);
        var quizzes = new SqliteQuizRepository(db);
        var flashcards = new SqliteFlashcardRepository(db);
        var plans = new SqliteStudyPlanRepository(db);
        var reminders = new SqliteReminderRepository(db);
        var history = new SqliteLearningHistoryRepository(db);

        RetrievalService retrieval = new RetrievalService(embeddings, new SqliteVectorStore(db, materials));
        ContextBuilder contextBuilder = new ContextBuilder(config.getInt("CONTEXT_TOKEN_BUDGET", 4000));
        ResponseValidator validator = new ResponseValidator();
        MemoryManager memory = new MemoryManager(students, courses, progress, history, config.getInt("MEMORY_MAX_TURNS", 20));
        ReminderScheduler reminderScheduler = new ReminderScheduler(reminders, gateway, history);

        ToolRegistry tools = new ToolRegistry();
        tools.register(new RetrievalTool(retrieval));
        tools.register(new ProgressAnalysisTool(concepts, progress));
        tools.register(new StudyScheduleTool());
        tools.register(new SummarizationTool(llm, contextBuilder, validator));
        tools.register(new ConceptExtractionTool(llm, contextBuilder, validator));
        tools.register(new FlashcardGenerationTool(llm, contextBuilder, validator));
        tools.register(new QuizGenerationTool(llm, contextBuilder, validator));
        tools.register(new ReminderTool(reminderScheduler::schedule, LocalTime.parse(config.get("REMINDER_TIME", "18:00"))));

        LearningAgent agent = new LearningAgent(new Planner(llm, contextBuilder), tools, memory, contextBuilder, validator,
                llm, materials, concepts, progress);
        ProgressTracker tracker = new ProgressTracker(progress, concepts, plans);
        StudyPlanMonitor monitor = new StudyPlanMonitor(agent, plans, courses, history);
        tracker.attach(monitor);
        tracker.attach(reminderScheduler);

        this.facade = new LearningFacade(
                new CourseService(courses, materials, concepts, progress, quizzes, flashcards, plans, retrieval),
                new MaterialService(List.of(new PdfParser(), new DocxParser(), new PlainTextParser()), new Chunker(), materials, retrieval),
                new GradingService(llm, contextBuilder, validator), tracker, reminderScheduler,
                new ConceptMapService(concepts, progress), monitor, agent, memory, plans, quizzes, flashcards, students, llm);

        this.studentId = config.get("STUDENT_ID", "student-1");
        if (students.findById(studentId).isEmpty()) {
            students.save(new Student(studentId, config.get("STUDENT_NAME", System.getProperty("user.name", "Student")), null));
        }
    }

    /** Production wiring from .env / environment: SQLite file, configured LLM provider and embeddings. */
    public static AppContext create(AppConfig config, NotificationGateway gateway) {
        return new AppContext(config, SqliteDatabase.open(config.databasePath()), LLMClientFactory.create(config),
                embeddings(config), gateway);
    }

    /** Wiring for tests and demos: given database and LLM, local embeddings. */
    public static AppContext create(AppConfig config, SqliteDatabase db, LLMClient llm, NotificationGateway gateway) {
        return new AppContext(config, db, llm, new LocalHashEmbeddingService(), gateway);
    }

    /**
     * Local hashed embeddings by default (no network, no quota); Gemini embeddings when
     * EMBEDDING_PROVIDER=gemini and a key is configured. Vectors record their model, so switching is safe.
     */
    static EmbeddingService embeddings(AppConfig config) {
        if (config.get("EMBEDDING_PROVIDER", "local").equalsIgnoreCase("gemini") && config.hasKey("GEMINI_API_KEY")) {
            GeminiSdkClient sdk = new GeminiSdkClient(config.get("GEMINI_API_KEY", ""),
                    Duration.ofSeconds(config.getInt("LLM_TIMEOUT_SECONDS", 60)));
            return new GeminiEmbeddingService(sdk, config.get("GEMINI_EMBEDDING_MODEL", "text-embedding-004"));
        }
        return new LocalHashEmbeddingService();
    }

    public LearningFacade facade() { return facade; }

    /** The local single-user profile all actions are recorded for. */
    public String studentId() { return studentId; }

    public AppConfig config() { return config; }

    @Override
    public void close() {
        facade.stopReminderTimer();
        db.close();
    }
}
