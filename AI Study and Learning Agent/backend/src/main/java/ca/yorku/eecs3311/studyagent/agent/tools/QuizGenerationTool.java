package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.agent.AgentContext;
import ca.yorku.eecs3311.studyagent.agent.ContextBuilder;
import ca.yorku.eecs3311.studyagent.agent.PromptTask;
import ca.yorku.eecs3311.studyagent.agent.ResponseValidator;
import ca.yorku.eecs3311.studyagent.agent.ValidationResult;
import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.DifficultyLevel;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionSpec;
import ca.yorku.eecs3311.studyagent.domain.QuestionType;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.llm.OutputSchema;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Client of the Factory Method pattern: asks the LLM for QuestionSpec JSON and turns each spec into a
 * concrete Question through the factory registered for its type. Rejected specs are replaced in up to
 * two more rounds; the result may contain fewer questions than requested.
 */
public class QuizGenerationTool implements Tool {
    public static final OutputSchema SCHEMA = new OutputSchema("quiz",
            "{\"questions\":[{\"type\":\"MULTIPLE_CHOICE|TRUE_FALSE|SHORT_ANSWER\",\"prompt\":\"string\",\"options\":[\"string\"],"
                    + "\"answer\":\"string\",\"rubric\":[\"string\"],\"conceptName\":\"string\",\"difficulty\":\"FOUNDATION|INTERMEDIATE|ADVANCED\"}]}",
            List.of("questions"));
    static final int MAX_ROUNDS = 3;

    private final LLMClient llm;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator validator;
    private final Map<QuestionType, QuestionFactory> factories = new EnumMap<>(QuestionType.class);
    private final Gson gson = new Gson();

    public QuizGenerationTool(LLMClient llm, ContextBuilder contextBuilder, ResponseValidator validator) {
        this.llm = llm;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
        factories.put(QuestionType.MULTIPLE_CHOICE, new MultipleChoiceQuestionFactory());
        factories.put(QuestionType.TRUE_FALSE, new TrueFalseQuestionFactory());
        factories.put(QuestionType.SHORT_ANSWER, new ShortAnswerQuestionFactory());
    }

    @Override public String getName() { return "quiz_generation"; }
    @Override public String getDescription() { return "Generate validated quiz questions of the requested types and difficulty"; }

    @Override
    public ToolResult execute(ToolInput input) {
        AgentContext ctx = input.get("context");
        int count = input.get("count", 5);
        List<QuestionType> types = input.get("types", List.of(QuestionType.values()));
        DifficultyLevel difficulty = input.get("difficulty", DifficultyLevel.INTERMEDIATE);
        List<Concept> concepts = input.get("concepts", List.of());

        List<Question> accepted = new ArrayList<>();
        Set<String> seenPrompts = new HashSet<>();
        int rejected = 0;
        List<String> reasons = new ArrayList<>();
        for (int round = 0; round < MAX_ROUNDS && accepted.size() < count; round++) {
            int missing = count - accepted.size();
            ctx.set("count", String.valueOf(missing))
               .set("difficulty", difficulty.name())
               .set("types", types.stream().map(Enum::name).collect(Collectors.joining(", ")))
               .set("concepts", concepts.isEmpty() ? "(any topic in the material)"
                       : concepts.stream().map(Concept::getName).collect(Collectors.joining(", ")))
               .set("extra", round == 0 ? input.getString("extra") == null ? "" : input.getString("extra")
                       : "Previous questions were rejected for: " + String.join("; ", reasons) + ". Avoid these problems.");
            ValidationResult v;
            try {
                v = validator.validate(llm.generateStructured(contextBuilder.build(PromptTask.GENERATE_QUIZ, ctx, 0.5, 4096), SCHEMA), SCHEMA);
            } catch (LLMException e) {
                if (accepted.isEmpty()) return ToolResult.fail("Question generation unavailable: " + e.getMessage());
                break;
            }
            if (!v.valid) {
                reasons.add(String.join(", ", v.errors));
                continue;
            }
            for (JsonElement e : v.json.getAsJsonArray("questions")) {
                if (accepted.size() >= count) break;
                try {
                    QuestionSpec spec = toSpec(e);
                    if (!types.contains(spec.type)) throw new ValidationException("type " + spec.type + " not requested");
                    resolveConcept(spec, concepts);
                    Question q = factories.get(spec.type).build(spec);
                    if (seenPrompts.add(q.getPrompt().toLowerCase(Locale.ROOT))) accepted.add(q);
                } catch (ValidationException | IllegalArgumentException ex) {
                    rejected++;
                    reasons.add(ex.getMessage());
                }
            }
        }
        if (accepted.isEmpty()) return ToolResult.fail("No valid questions could be generated (" + String.join("; ", reasons) + ")");
        String msg = accepted.size() + " question(s) accepted, " + rejected + " rejected by validation";
        return ToolResult.ok(accepted, msg);
    }

    private QuestionSpec toSpec(JsonElement e) {
        QuestionSpec spec = new QuestionSpec();
        var o = e.getAsJsonObject();
        if (!o.has("type")) throw new ValidationException("question without type");
        spec.type = QuestionType.valueOf(o.get("type").getAsString().trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        spec.prompt = o.has("prompt") ? o.get("prompt").getAsString() : null;
        spec.answer = o.has("answer") && !o.get("answer").isJsonNull() ? o.get("answer").getAsString() : null;
        spec.conceptName = o.has("conceptName") && !o.get("conceptName").isJsonNull() ? o.get("conceptName").getAsString() : null;
        spec.options = o.has("options") && o.get("options").isJsonArray() ? gson.fromJson(o.get("options"), List.class) : null;
        spec.rubric = o.has("rubric") && o.get("rubric").isJsonArray() ? gson.fromJson(o.get("rubric"), List.class) : null;
        if (o.has("difficulty") && !o.get("difficulty").isJsonNull()) {
            spec.difficulty = DifficultyLevel.valueOf(o.get("difficulty").getAsString().trim().toUpperCase(Locale.ROOT));
        }
        return spec;
    }

    /** Unknown concept names are rejected when the quiz targets specific concepts. */
    static void resolveConcept(QuestionSpec spec, List<Concept> concepts) {
        if (concepts.isEmpty()) return;
        Optional<Concept> match = concepts.stream()
                .filter(c -> c.normalizedName().equals(Concept.normalize(spec.conceptName))).findFirst();
        if (match.isEmpty() && concepts.size() == 1) match = Optional.of(concepts.get(0));
        Concept c = match.orElseThrow(() -> new ValidationException("unknown concept '" + spec.conceptName + "'"));
        spec.conceptId = c.getId();
        spec.conceptName = c.getName();
    }

    public QuestionFactory factoryFor(QuestionType type) { return factories.get(type); }
}
