package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.agent.AgentContext;
import ca.yorku.eecs3311.studyagent.agent.ContextBuilder;
import ca.yorku.eecs3311.studyagent.agent.PromptTask;
import ca.yorku.eecs3311.studyagent.agent.ResponseValidator;
import ca.yorku.eecs3311.studyagent.agent.ValidationResult;
import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.Flashcard;
import ca.yorku.eecs3311.studyagent.domain.FlashcardDeck;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.llm.OutputSchema;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Generates a deck from retrieved material; empty and duplicate cards are dropped by validation. */
public class FlashcardGenerationTool implements Tool {
    public static final OutputSchema SCHEMA = new OutputSchema("flashcards",
            "{\"cards\":[{\"front\":\"string\",\"back\":\"string\",\"conceptName\":\"string\"}]}", List.of("cards"));

    private final LLMClient llm;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator validator;

    public FlashcardGenerationTool(LLMClient llm, ContextBuilder contextBuilder, ResponseValidator validator) {
        this.llm = llm;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
    }

    @Override public String getName() { return "flashcard_generation"; }
    @Override public String getDescription() { return "Write spaced-repetition flashcards from course material"; }

    @Override
    public ToolResult execute(ToolInput input) {
        AgentContext ctx = input.get("context");
        int count = input.get("count", 10);
        String topic = input.getString("topic");
        List<Concept> concepts = input.get("concepts", List.of());
        ctx.set("count", String.valueOf(count)).set("topic", topic)
           .set("concepts", concepts.isEmpty() ? topic : concepts.stream().map(Concept::getName).collect(Collectors.joining(", ")));
        ValidationResult v;
        try {
            v = validator.validate(llm.generateStructured(contextBuilder.build(PromptTask.GENERATE_FLASHCARDS, ctx, 0.4, 3000), SCHEMA), SCHEMA);
        } catch (LLMException e) {
            return ToolResult.fail("Flashcard generation unavailable: " + e.getMessage());
        }
        if (!v.valid) return ToolResult.fail("The AI returned invalid flashcards: " + String.join(", ", v.errors));

        FlashcardDeck deck = new FlashcardDeck(Ids.newId("d"), input.getString("courseId"), "Flashcards: " + topic);
        Set<String> fronts = new HashSet<>();
        int dropped = 0;
        for (JsonElement e : v.json.getAsJsonArray("cards")) {
            if (deck.getCards().size() >= count) break;
            JsonObject o = e.getAsJsonObject();
            String front = text(o, "front");
            String back = text(o, "back");
            if (!fronts.add(front.toLowerCase(Locale.ROOT))) {
                dropped++;
                continue;
            }
            try {
                String conceptId = match(text(o, "conceptName"), concepts).map(Concept::getId)
                        .orElse(concepts.size() == 1 ? concepts.get(0).getId() : null);
                deck.add(new Flashcard(Ids.newId("f"), deck.getId(), conceptId, front, back));
            } catch (ValidationException ex) {
                dropped++;
            }
        }
        if (deck.getCards().isEmpty()) return ToolResult.fail("No valid flashcards were generated");
        if (deck.getCards().size() < count) {
            deck.setNotice("Created " + deck.getCards().size() + " of " + count + " requested cards (" + dropped + " invalid or duplicate).");
        }
        return ToolResult.ok(deck, deck.getCards().size() + " card(s), " + dropped + " dropped");
    }

    private static Optional<Concept> match(String name, List<Concept> concepts) {
        String key = Concept.normalize(name);
        return concepts.stream().filter(c -> c.normalizedName().equals(key)).findFirst();
    }

    private static String text(JsonObject o, String field) {
        return o.has(field) && !o.get(field).isJsonNull() ? o.get(field).getAsString().trim() : "";
    }
}
