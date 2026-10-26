package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.MultipleChoiceQuestion;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionType;
import ca.yorku.eecs3311.studyagent.domain.ShortAnswerQuestion;
import ca.yorku.eecs3311.studyagent.domain.TrueFalseQuestion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Gson configuration for storing domain objects as JSON documents. */
public final class JsonCodec {
    private JsonCodec() {}

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (v, t, c) -> new JsonPrimitive(v.toString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (j, t, c) -> LocalDate.parse(j.getAsString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (v, t, c) -> new JsonPrimitive(v.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (j, t, c) -> LocalDateTime.parse(j.getAsString()))
            // Question is abstract: store the concrete type next to the data.
            .registerTypeAdapter(Question.class, (JsonSerializer<Question>) (q, t, c) -> {
                JsonObject o = new JsonObject();
                o.addProperty("kind", q.getType().name());
                o.add("data", c.serialize(q, q.getClass()));
                return o;
            })
            .registerTypeAdapter(Question.class, (JsonDeserializer<Question>) (j, t, c) -> {
                JsonObject o = j.getAsJsonObject();
                Class<? extends Question> cls = switch (QuestionType.valueOf(o.get("kind").getAsString())) {
                    case MULTIPLE_CHOICE -> MultipleChoiceQuestion.class;
                    case TRUE_FALSE -> TrueFalseQuestion.class;
                    case SHORT_ANSWER -> ShortAnswerQuestion.class;
                };
                return c.deserialize(o.get("data"), cls);
            })
            .create();
}
