package ca.yorku.eecs3311.studyagent.integration.llm;

import java.util.List;

/**
 * Expected JSON shape of a structured LLM answer. The jsonSchema text is included in the prompt;
 * requiredFields are checked by the agent's ResponseValidator.
 */
public class OutputSchema {
    public final String name;
    public final String jsonSchema;
    public final List<String> requiredFields;

    public OutputSchema(String name, String jsonSchema, List<String> requiredFields) {
        this.name = name;
        this.jsonSchema = jsonSchema;
        this.requiredFields = List.copyOf(requiredFields);
    }
}
