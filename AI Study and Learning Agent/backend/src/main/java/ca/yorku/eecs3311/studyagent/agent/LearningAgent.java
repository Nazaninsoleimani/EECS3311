package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.agent.tools.StudyScheduleTool;
import ca.yorku.eecs3311.studyagent.agent.tools.ToolInput;
import ca.yorku.eecs3311.studyagent.agent.tools.ToolResult;
import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.AdaptiveLearningSession;
import ca.yorku.eecs3311.studyagent.domain.Answer;
import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.domain.Explanation;
import ca.yorku.eecs3311.studyagent.domain.FlashcardDeck;
import ca.yorku.eecs3311.studyagent.domain.FlashcardRequest;
import ca.yorku.eecs3311.studyagent.domain.GradeResult;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.PerformanceRecord;
import ca.yorku.eecs3311.studyagent.domain.ProgressEvent;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.domain.QuizRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.SummaryOptions;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import ca.yorku.eecs3311.studyagent.domain.WeakTopic;
import ca.yorku.eecs3311.studyagent.domain.WeakTopicReport;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.llm.OutputSchema;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import ca.yorku.eecs3311.studyagent.persistence.ConceptRepository;
import ca.yorku.eecs3311.studyagent.persistence.MaterialRepository;
import ca.yorku.eecs3311.studyagent.persistence.ProgressRepository;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * The agent loop: observe (load learner context), plan (Planner), act (execute tool steps and
 * revise the plan after each observation), generate (LLM with retrieved sources), evaluate
 * (ResponseValidator) and remember (session memory). Every goal of the application goes through here.
 */
public class LearningAgent {
    static final OutputSchema PRIORITY_SCHEMA = new OutputSchema("priorities",
            "{\"topics\":[{\"name\":\"string\",\"priority\":0.5,\"objective\":\"string\",\"reason\":\"string\"}],\"rationale\":\"string\"}",
            List.of("topics"));
    static final OutputSchema DIAGNOSIS_SCHEMA = new OutputSchema("diagnoses",
            "{\"diagnoses\":[{\"conceptName\":\"string\",\"diagnosis\":\"string\",\"recommendedActivity\":\"READ|FLASHCARDS|QUIZ|ADAPTIVE_SESSION\"}]}",
            List.of("diagnoses"));
    static final String NOT_COVERED = "The course material does not cover this.";

    private final Planner planner;
    private final ToolRegistry tools;
    private final MemoryManager memory;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator validator;
    private final LLMClient llm;
    private final MaterialRepository materials;
    private final ConceptRepository concepts;
    private final ProgressRepository progress;
    private volatile String lastTrace = "";

    public LearningAgent(Planner planner, ToolRegistry tools, MemoryManager memory, ContextBuilder contextBuilder,
                         ResponseValidator validator, LLMClient llm, MaterialRepository materials,
                         ConceptRepository concepts, ProgressRepository progress) {
        this.planner = planner;
        this.tools = tools;
        this.memory = memory;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
        this.llm = llm;
        this.materials = materials;
        this.concepts = concepts;
        this.progress = progress;
    }

    // ------------------------------------------------------------------ F05 tutor Q&A

    public Answer answerQuestion(String studentId, String sessionId, String courseId, String question) {
        if (question == null || question.isBlank()) throw new AgentException("Please type a question");
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(studentId, courseId));
        String history = formatHistory(memory.getSessionMemory(sessionId).recent(4));
        AgentGoal goal = new AgentGoal(GoalType.ANSWER_QUESTION, question)
                .with("courseId", courseId).with("question", question).with("history", history);
        AgentPlan plan = planner.createPlan(goal, ctx);
        executePlan(plan, ctx, (step, in) -> {});

        Answer answer;
        if (plan.hasNote("no-grounding")) {
            answer = new Answer(NOT_COVERED + " No uploaded material for this course matches the question.", List.of(), false);
        } else if (!llm.isAvailable()) {
            answer = extractiveAnswer(ctx.retrieved);
        } else {
            answer = generateGroundedAnswer(ctx, question, history);
        }
        memory.appendToSession(sessionId, new ConversationTurn(question, answer.text));
        return answer;
    }

    /** Generates, checks citations, and regenerates once with a stricter instruction if grounding fails. */
    private Answer generateGroundedAnswer(AgentContext ctx, String question, String history) {
        ctx.set("question", question).set("history", history.isBlank() ? "(none)" : history).set("strict", "");
        List<RetrievedChunk> sources = contextBuilder.rankAndTrim(ctx.retrieved);
        try {
            String text = llm.generate(contextBuilder.build(PromptTask.ANSWER_WITH_CITATIONS, ctx)).text.trim();
            if (!validator.checkGrounding(text, sources) && !text.contains(NOT_COVERED)) {
                ctx.trace.add("answer failed grounding check; regenerating with strict citation instruction");
                ctx.set("strict", "IMPORTANT: your previous answer had missing or invalid citations. Every sentence must cite one of the numbered sources.");
                text = llm.generate(contextBuilder.build(PromptTask.ANSWER_WITH_CITATIONS, ctx, 0.0, 2048)).text.trim();
            }
            boolean grounded = validator.checkGrounding(text, sources);
            List<String> citations = validator.citedSources(text).stream()
                    .filter(n -> n >= 1 && n <= sources.size())
                    .map(n -> "[" + n + "] " + sources.get(n - 1).chunk.sourceLabel()).toList();
            if (!grounded && !text.contains(NOT_COVERED)) text = "(Warning: this answer could not be verified against your material.)\n" + text;
            return new Answer(text, citations, grounded);
        } catch (LLMException e) {
            ctx.trace.add("LLM unavailable: " + e.getMessage());
            return extractiveAnswer(ctx.retrieved);
        }
    }

    /** Deterministic fallback: the most relevant passages, quoted with their sources. */
    static Answer extractiveAnswer(List<RetrievedChunk> retrieved) {
        StringBuilder sb = new StringBuilder("The AI model is unavailable, so here are the most relevant passages from your material:\n");
        List<String> citations = new ArrayList<>();
        for (int i = 0; i < Math.min(3, retrieved.size()); i++) {
            RetrievedChunk c = retrieved.get(i);
            String text = c.chunk.getText();
            sb.append("\n[").append(i + 1).append("] ").append(text, 0, Math.min(400, text.length()))
              .append(text.length() > 400 ? "..." : "").append('\n');
            citations.add("[" + (i + 1) + "] " + c.chunk.sourceLabel());
        }
        return new Answer(sb.toString().trim(), citations, true);
    }

    private static String formatHistory(List<ConversationTurn> turns) {
        return turns.stream().map(t -> "Student: " + t.question + "\nTutor: " + t.answer).collect(Collectors.joining("\n"));
    }

    // ------------------------------------------------------------------ F03 summary, F04 concepts

    public Summary summarize(String studentId, CourseMaterial material, SummaryOptions options) {
        List<DocumentChunk> chunks = indexedChunks(material);
        int tokens = chunks.stream().mapToInt(c -> ContextBuilder.estimateTokens(c.getText())).sum();
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(studentId, material.getCourseId()));
        AgentPlan plan = planner.createPlan(new AgentGoal(GoalType.SUMMARIZE, material.getTitle()).with("totalTokens", tokens), ctx);
        executePlan(plan, ctx, (step, in) -> in.with("chunks", chunks).with("options", options)
                .with("materialId", material.getId()).with("title", material.getTitle()));
        return ctx.results.get("summarize").payload();
    }

    /** Returns the concepts and relations found in the material; persisting them is the caller's job. */
    public ConceptGraph extractConcepts(String studentId, CourseMaterial material) {
        List<DocumentChunk> chunks = indexedChunks(material);
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(studentId, material.getCourseId()));
        AgentPlan plan = planner.createPlan(new AgentGoal(GoalType.EXTRACT_CONCEPTS, material.getTitle()), ctx);
        List<Concept> known = concepts.findByCourse(material.getCourseId());
        executePlan(plan, ctx, (step, in) -> in.with("chunks", chunks).with("courseId", material.getCourseId())
                .with("materialId", material.getId()).with("known", known));
        return ctx.results.get("extract").payload();
    }

    private List<DocumentChunk> indexedChunks(CourseMaterial material) {
        List<DocumentChunk> chunks = materials.findChunks(material.getId());
        if (chunks.isEmpty()) throw new AgentException("'" + material.getTitle() + "' has no extracted text to work with");
        return chunks;
    }

    // ------------------------------------------------------------------ F06 study plan

    public StudyPlan generateStudyPlan(StudyPlanRequest request) {
        return buildPlan(request, GoalType.GENERATE_STUDY_PLAN, null);
    }

    /** Builds a replacement plan from today that puts the concept whose mastery dropped first. */
    public StudyPlan revisePlan(StudyPlan plan, ProgressEvent event, LocalDate examDate) {
        int dailyMinutes = plan.getTasks().stream().collect(Collectors.groupingBy(StudyTask::getDate,
                Collectors.summingInt(StudyTask::getDurationMinutes))).values().stream().mapToInt(Integer::intValue).max().orElse(60);
        LocalDate exam = examDate != null && examDate.isAfter(LocalDate.now()) ? examDate : null;
        StudyPlanRequest request = new StudyPlanRequest(plan.getStudentId(), plan.getCourseId(), exam, dailyMinutes,
                List.of(event.conceptName));
        String why = String.format("Revised because mastery of %s changed from %.0f%% to %.0f%%.",
                event.conceptName, event.oldMastery * 100, event.newMastery * 100);
        StudyPlan revised = buildPlan(request, GoalType.REVISE_STUDY_PLAN, why);
        revised.setRevision(plan.getRevision() + 1);
        return revised;
    }

    private StudyPlan buildPlan(StudyPlanRequest request, GoalType type, String revisionReason) {
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(request.studentId, request.courseId));
        boolean reminders = ctx.learner.profile.isRemindersEnabled() && tools.has("reminder");
        AgentGoal goal = new AgentGoal(type, "Study plan").with("courseId", request.courseId)
                .with("focusQuery", String.join(" ", request.focusTopics)).with("reminders", reminders);
        AgentPlan plan = planner.createPlan(goal, ctx);
        StudyPlan[] built = new StudyPlan[1];
        List<String> warnings = new ArrayList<>();
        executePlan(plan, ctx, (step, in) -> {
            switch (step.toolName) {
                case "study_schedule" -> {
                    List<TopicPriority> ranked = ctx.results.get("analyze").payload();
                    in.with("request", request).with("priorities", prioritize(ctx, request, ranked, warnings));
                }
                case "reminder" -> {
                    built[0] = toPlan(ctx, request, revisionReason, warnings);
                    in.with("plan", built[0]);
                }
                default -> { }
            }
        });
        return built[0] != null ? built[0] : toPlan(ctx, request, revisionReason, warnings);
    }

    private StudyPlan toPlan(AgentContext ctx, StudyPlanRequest request, String revisionReason, List<String> warnings) {
        StudyScheduleTool.Schedule schedule = ctx.results.get("schedule").payload();
        String rationale = ctx.variables.getOrDefault("rationale", "");
        if (rationale.isBlank()) {
            rationale = schedule.strategyName() + " strategy for " + request.availableDays()
                    + " day(s); weakest and least recently practised concepts come first.";
        }
        if (revisionReason != null) rationale = revisionReason + " " + rationale;
        StudyPlan plan = new StudyPlan(Ids.newId("p"), request.studentId, request.courseId, schedule.strategyName(),
                rationale, schedule.tasks());
        plan.getUnscheduledTopics().addAll(schedule.unscheduledTopics());
        plan.getWarnings().addAll(warnings);
        if (!schedule.unscheduledTopics().isEmpty()) {
            plan.getWarnings().add(schedule.unscheduledTopics().size() + " topic(s) did not fit into the available time");
        }
        return plan;
    }

    /**
     * Deterministic ranking first, focus topics boosted; then the LLM may reorder the candidates and write
     * objectives. Names the LLM invents are ignored and candidates it leaves out keep their place at the end.
     */
    List<TopicPriority> prioritize(AgentContext ctx, StudyPlanRequest request, List<TopicPriority> ranked, List<String> warnings) {
        List<TopicPriority> ordered = new ArrayList<>(ranked);
        Set<String> focus = request.focusTopics.stream().map(Concept::normalize).collect(Collectors.toSet());
        ordered.sort(Comparator.comparingDouble((TopicPriority t) -> t.priority + (focus.contains(t.concept.normalizedName()) ? 1.0 : 0.0)).reversed());
        if (!llm.isAvailable()) {
            warnings.add("AI prioritisation unavailable; deterministic ranking used");
            return ordered;
        }
        ctx.set("days", String.valueOf(request.availableDays())).set("minutes", String.valueOf(request.dailyMinutes))
           .set("focus", request.focusTopics.isEmpty() ? "(none)" : String.join(", ", request.focusTopics))
           .set("candidates", ordered.stream().map(t -> String.format("- %s: %.2f (%s)", t.concept.getName(), t.priority, t.reason))
                   .collect(Collectors.joining("\n")));
        try {
            ValidationResult v = validator.validate(llm.generateStructured(
                    contextBuilder.build(PromptTask.PRIORITIZE_TOPICS, ctx, 0.2, 2500), PRIORITY_SCHEMA), PRIORITY_SCHEMA);
            if (!v.valid) {
                warnings.add("AI prioritisation returned invalid output; deterministic ranking used");
                return ordered;
            }
            Map<String, TopicPriority> byName = new LinkedHashMap<>();
            ordered.forEach(t -> byName.put(t.concept.normalizedName(), t));
            List<TopicPriority> result = new ArrayList<>();
            for (JsonElement e : v.json.getAsJsonArray("topics")) {
                JsonObject o = e.getAsJsonObject();
                TopicPriority t = byName.remove(Concept.normalize(text(o, "name")));
                if (t == null) continue;
                if (!text(o, "objective").isBlank()) t.objective = text(o, "objective");
                if (!text(o, "reason").isBlank()) t.reason = text(o, "reason");
                result.add(t);
            }
            result.addAll(byName.values());
            if (v.json.has("rationale")) ctx.set("rationale", text(v.json, "rationale"));
            ctx.trace.add("LLM ordered " + (result.size() - byName.size()) + " of " + result.size() + " topics");
            return result;
        } catch (LLMException e) {
            warnings.add("AI prioritisation unavailable (" + e.getMessage() + "); deterministic ranking used");
            return ordered;
        }
    }

    // ------------------------------------------------------------------ F07 flashcards, F08 quiz

    public FlashcardDeck generateFlashcards(FlashcardRequest request) {
        List<Concept> selected = selectConcepts(request.courseId, request.conceptIds);
        String topic = topicOf(request.topic, selected);
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(request.studentId, request.courseId));
        AgentPlan plan = planner.createPlan(new AgentGoal(GoalType.GENERATE_FLASHCARDS, topic)
                .with("courseId", request.courseId).with("topic", topic), ctx);
        executePlan(plan, ctx, (step, in) -> in.with("count", request.count).with("topic", topic)
                .with("concepts", selected).with("courseId", request.courseId));
        if (plan.hasNote("no-grounding")) {
            throw new AgentException("No course material matches '" + topic + "'. Upload material or choose another topic.");
        }
        return ctx.results.get("generate").payload();
    }

    public Quiz generateQuiz(QuizRequest request) {
        List<Concept> chosen = selectConcepts(request.courseId, request.conceptIds);
        String topic = topicOf(null, chosen);
        // a whole-course quiz targets the course's known concepts so every answer updates mastery
        List<Concept> selected = chosen.isEmpty() ? concepts.findByCourse(request.courseId).stream().limit(15).toList() : chosen;
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(request.studentId, request.courseId));
        AgentPlan plan = planner.createPlan(new AgentGoal(GoalType.GENERATE_QUIZ, topic)
                .with("courseId", request.courseId).with("topic", topic), ctx);
        executePlan(plan, ctx, (step, in) -> {
            if (step.toolName.equals("retrieval") && topic.isBlank()) {
                in.with("query", selected.stream().map(Concept::getName).collect(Collectors.joining(" ")));
            }
            if (step.toolName.equals("quiz_generation")) {
                if (ctx.retrieved.isEmpty()) sampleCourseChunks(ctx, request.courseId, 6);
                if (ctx.retrieved.isEmpty()) throw new AgentException("This course has no indexed material to build a quiz from");
            }
            in.with("count", request.count).with("types", request.types).with("difficulty", request.difficulty).with("concepts", selected);
        });
        List<Question> questions = ctx.results.get("generate").payload();
        Quiz quiz = new Quiz(Ids.newId("qz"), request.courseId, request.difficulty, questions);
        if (questions.size() < request.count) {
            quiz.setNotice("Generated " + questions.size() + " of " + request.count + " requested questions; the others failed validation.");
        } else if (selected.isEmpty()) {
            quiz.setNotice("No concepts extracted for this course yet, so this quiz will not update concept mastery.");
        }
        return quiz;
    }

    /** Whole-course quiz without a usable query: spread the sources evenly over all indexed chunks. */
    private void sampleCourseChunks(AgentContext ctx, String courseId, int k) {
        List<DocumentChunk> all = materials.findChunksByCourse(courseId);
        if (all.isEmpty()) return;
        int stride = Math.max(1, all.size() / k);
        for (int i = 0; i < all.size() && ctx.retrieved.size() < k; i += stride) ctx.retrieved.add(new RetrievedChunk(all.get(i), 1.0));
        ctx.trace.add("retrieval found nothing for a whole-course quiz; sampled " + ctx.retrieved.size() + " chunk(s) across the material");
    }

    private List<Concept> selectConcepts(String courseId, List<String> conceptIdsOrNames) {
        if (conceptIdsOrNames.isEmpty()) return List.of();
        List<Concept> all = concepts.findByCourse(courseId);
        List<Concept> selected = new ArrayList<>();
        for (String key : conceptIdsOrNames) {
            all.stream().filter(c -> c.getId().equals(key) || c.normalizedName().equals(Concept.normalize(key)))
                    .findFirst().ifPresentOrElse(selected::add, () -> {
                        throw new AgentException("Unknown concept '" + key + "' in this course");
                    });
        }
        return selected;
    }

    private static String topicOf(String topic, List<Concept> selected) {
        if (topic != null && !topic.isBlank()) return topic.trim();
        return selected.stream().map(Concept::getName).collect(Collectors.joining(", "));
    }

    // ------------------------------------------------------------------ F10 explanation

    public Explanation explainAnswer(String studentId, String courseId, Question question, QuestionResponse response, GradeResult grade) {
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(studentId, courseId));
        AgentPlan plan = planner.createPlan(new AgentGoal(GoalType.EXPLAIN_ANSWER, question.getPrompt())
                .with("courseId", courseId).with("question", question.getPrompt()), ctx);
        executePlan(plan, ctx, (step, in) -> {});
        String given = response == null || response.answerText == null || response.answerText.isBlank() ? "(no answer)" : response.answerText;
        List<RetrievedChunk> sources = contextBuilder.rankAndTrim(ctx.retrieved);
        if (llm.isAvailable()) {
            ctx.set("question", question.getPrompt()).set("correct", question.correctAnswerText()).set("answer", given);
            try {
                String text = llm.generate(contextBuilder.build(PromptTask.EXPLAIN_ANSWER, ctx, 0.2, 800)).text.trim();
                List<String> refs = validator.citedSources(text).stream().filter(n -> n >= 1 && n <= sources.size())
                        .map(n -> "[" + n + "] " + sources.get(n - 1).chunk.sourceLabel()).toList();
                return new Explanation(text, refs, !refs.isEmpty());
            } catch (LLMException e) {
                ctx.trace.add("LLM unavailable for explanation: " + e.getMessage());
            }
        }
        StringBuilder sb = new StringBuilder("Correct answer: ").append(question.correctAnswerText()).append('.');
        if (grade != null && grade.feedback != null) sb.append("\n").append(grade.feedback);
        List<String> refs = new ArrayList<>();
        if (!sources.isEmpty()) {
            String text = sources.get(0).chunk.getText();
            sb.append("\n\nFrom your material [1]: ").append(text, 0, Math.min(350, text.length())).append(text.length() > 350 ? "..." : "");
            refs.add("[1] " + sources.get(0).chunk.sourceLabel());
        }
        return new Explanation(sb.toString(), refs, !refs.isEmpty());
    }

    // ------------------------------------------------------------------ F11 weak topics

    public WeakTopicReport diagnoseWeakTopics(String studentId, String courseId) {
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(studentId, courseId));
        AgentPlan plan = planner.createPlan(new AgentGoal(GoalType.DIAGNOSE_WEAK_TOPICS, "weak topics").with("courseId", courseId), ctx);
        executePlan(plan, ctx, (step, in) -> {});
        List<TopicPriority> ranked = ctx.results.get("analyze").payload();
        Map<String, ConceptMastery> mastery = new HashMap<>();
        ctx.learner.masteries.forEach(m -> mastery.put(m.getConceptId(), m));
        boolean sufficient = mastery.values().stream().anyMatch(m -> m.getAttempts() >= ConceptMastery.MIN_ATTEMPTS_FOR_WEAK);
        WeakTopicReport report = new WeakTopicReport(courseId, sufficient);
        for (TopicPriority t : ranked) {
            ConceptMastery m = mastery.get(t.concept.getId());
            boolean weak = m != null && (m.isWeak() || !sufficient && m.getAttempts() > 0 && m.getMastery() < ConceptMastery.WEAK_THRESHOLD);
            if (!weak) continue;
            report.topics.add(new WeakTopic(t.concept.getId(), t.concept.getName(), m.getMastery(), t.priority,
                    t.reason, recommendedActivity(m.getMastery())));
        }
        if (!sufficient) {
            report.message = "Not enough graded practice yet: a concept needs at least " + ConceptMastery.MIN_ATTEMPTS_FOR_WEAK
                    + " graded attempts before it can be called weak. Take a quiz or an adaptive session first.";
            if (report.topics.isEmpty()) {
                ranked.stream().limit(3).forEach(t -> report.topics.add(new WeakTopic(t.concept.getId(), t.concept.getName(),
                        0.0, t.priority, "Suggested next: " + t.reason, ActivityType.READ)));
            }
            return report;
        }
        if (report.topics.isEmpty()) {
            report.message = "No weak topics: every practised concept is at or above " + (int) (ConceptMastery.WEAK_THRESHOLD * 100) + "% mastery.";
            return report;
        }
        report.message = report.topics.size() + " weak topic(s), most urgent first.";
        if (llm.isAvailable()) diagnoseWithLlm(ctx, studentId, courseId, report);
        return report;
    }

    /** Adds an AI diagnosis of the likely misconception from the student's wrong answers. */
    private void diagnoseWithLlm(AgentContext ctx, String studentId, String courseId, WeakTopicReport report) {
        List<PerformanceRecord> records = progress.findPerformance(studentId, courseId);
        StringBuilder evidence = new StringBuilder();
        for (WeakTopic t : report.topics) {
            evidence.append("Concept: ").append(t.conceptName).append(String.format(" (mastery %.0f%%)%n", t.mastery * 100));
            records.stream().filter(r -> t.conceptId.equals(r.conceptId) && !r.correct).limit(3)
                    .forEach(r -> evidence.append("  Q: ").append(r.questionPrompt).append("\n  Student answered: ").append(r.studentAnswer).append('\n'));
        }
        ToolResult found = tools.getTool("retrieval").execute(ToolInput.of("query",
                report.topics.stream().map(t -> t.conceptName).collect(Collectors.joining(" ")), "courseId", courseId, "k", 4));
        if (found.success) ctx.retrieved.addAll(found.<List<RetrievedChunk>>payload());
        ctx.set("evidence", evidence.toString());
        try {
            ValidationResult v = validator.validate(llm.generateStructured(
                    contextBuilder.build(PromptTask.DIAGNOSE_WEAKNESS, ctx, 0.2, 1500), DIAGNOSIS_SCHEMA), DIAGNOSIS_SCHEMA);
            if (!v.valid) return;
            for (JsonElement e : v.json.getAsJsonArray("diagnoses")) {
                JsonObject o = e.getAsJsonObject();
                String key = Concept.normalize(text(o, "conceptName"));
                report.topics.stream().filter(t -> Concept.normalize(t.conceptName).equals(key)).findFirst().ifPresent(t -> {
                    if (!text(o, "diagnosis").isBlank()) t.diagnosis = text(o, "diagnosis");
                    try {
                        t.recommendedActivity = ActivityType.valueOf(text(o, "recommendedActivity").toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        // keep the deterministic recommendation
                    }
                });
            }
            report.aiDiagnosed = true;
        } catch (LLMException e) {
            ctx.trace.add("diagnosis unavailable: " + e.getMessage());
        }
    }

    static ActivityType recommendedActivity(double mastery) {
        if (mastery < 0.3) return ActivityType.READ;
        if (mastery < 0.5) return ActivityType.FLASHCARDS;
        return ActivityType.QUIZ;
    }

    // ------------------------------------------------------------------ F12 adaptive session

    /**
     * Picks the most urgent concept not asked in the last two activities and generates one question
     * whose type and difficulty come from the session's current DifficultyState.
     */
    public Question nextAdaptiveActivity(AdaptiveLearningSession session) {
        AgentContext ctx = new AgentContext(memory.loadLearnerContext(session.getStudentId(), session.getCourseId()));
        AgentPlan plan = planner.createPlan(new AgentGoal(GoalType.NEXT_ADAPTIVE_ACTIVITY, "next activity")
                .with("courseId", session.getCourseId()), ctx);
        Concept[] chosen = new Concept[1];
        executePlan(plan, ctx, (step, in) -> {
            if (step.toolName.equals("retrieval")) {
                List<TopicPriority> ranked = ctx.results.get("analyze").payload();
                chosen[0] = pickConcept(ranked, session.getRecentConceptIds());
                in.with("query", chosen[0].getName());
            } else if (step.toolName.equals("quiz_generation")) {
                in.with("count", 1).with("types", session.getState().preferredQuestionTypes())
                  .with("difficulty", session.getLevel()).with("concepts", List.of(chosen[0]));
            }
        });
        List<Question> generated = ctx.results.get("generate").payload();
        Question q = generated.get(0);
        session.setCurrentQuestion(q);
        return q;
    }

    static Concept pickConcept(List<TopicPriority> ranked, List<String> recentConceptIds) {
        List<String> avoid = recentConceptIds.subList(Math.max(0, recentConceptIds.size() - 2), recentConceptIds.size());
        return ranked.stream().map(t -> t.concept).filter(c -> !avoid.contains(c.getId())).findFirst()
                .orElse(ranked.get(0).concept);
    }

    // ------------------------------------------------------------------ loop

    /**
     * Executes the plan step by step. The binder adds goal-specific inputs (often derived from earlier
     * results); after each observation the Planner may revise the remaining steps. A failed required
     * step aborts the goal with the tool's message.
     */
    AgentContext executePlan(AgentPlan plan, AgentContext ctx, BiConsumer<PlanStep, ToolInput> binder) {
        try {
            for (PlanStep step = plan.nextStep(); step != null; step = plan.nextStep()) {
                ToolInput input = new ToolInput();
                input.parameters.putAll(step.input.parameters);
                input.with("context", ctx);
                ToolResult result;
                try {
                    binder.accept(step, input);
                    result = tools.getTool(step.toolName).execute(input);
                } catch (AgentException e) {
                    throw e;
                } catch (RuntimeException e) {
                    result = ToolResult.fail(step.name + " failed: " + e.getMessage());
                }
                ctx.add(step.name, result);
                step.status = result.success ? StepStatus.DONE : StepStatus.FAILED;
                if (result.success && step.toolName.equals("retrieval")) {
                    ctx.retrieved.addAll(result.<List<RetrievedChunk>>payload());
                    ctx.retrieved.sort(Comparator.comparingDouble((RetrievedChunk c) -> c.score).reversed());
                }
                if (!result.success && step.required) throw new AgentException(result.message);
                planner.revise(plan, step, result);
            }
            return ctx;
        } finally {
            lastTrace = plan.describe() + String.join("\n", ctx.trace);
        }
    }

    /** Plan and step outcomes of the most recent goal, for the CLI's --trace flag and the GUI's agent log. */
    public String getLastTrace() { return lastTrace; }

    public boolean isLlmAvailable() { return llm.isAvailable(); }

    public String getModelName() { return llm.getModelName(); }

    private static String text(JsonObject o, String field) {
        return o.has(field) && !o.get(field).isJsonNull() ? o.get(field).getAsString().trim() : "";
    }
}
