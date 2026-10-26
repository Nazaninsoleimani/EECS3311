package ca.yorku.eecs3311.studyagent.agent;

import com.google.gson.JsonObject;
import java.util.List;

public class ValidationResult {
    public final boolean valid;
    public final List<String> errors;
    /** Parsed JSON when valid (null otherwise). */
    public final JsonObject json;

    private ValidationResult(boolean valid, List<String> errors, JsonObject json) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
        this.json = json;
    }

    public static ValidationResult ok(JsonObject json) { return new ValidationResult(true, List.of(), json); }
    public static ValidationResult invalid(List<String> errors) { return new ValidationResult(false, errors, null); }
}
