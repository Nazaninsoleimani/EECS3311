package ca.yorku.eecs3311.studyagent.integration.retrieval;

import ca.yorku.eecs3311.studyagent.integration.llm.GeminiSdkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Semantic embeddings from the Gemini embedding model (batched, max 100 texts per call). */
public class GeminiEmbeddingService implements EmbeddingService {
    private final GeminiSdkClient sdk;
    private final String model;

    public GeminiEmbeddingService(GeminiSdkClient sdk, String model) {
        this.sdk = sdk;
        this.model = model;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> out = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += 100) {
            JsonArray requests = new JsonArray();
            for (String t : texts.subList(start, Math.min(texts.size(), start + 100))) {
                JsonObject req = new JsonObject();
                req.addProperty("model", "models/" + model);
                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", t);
                parts.add(part);
                content.add("parts", parts);
                req.add("content", content);
                requests.add(req);
            }
            JsonObject body = new JsonObject();
            body.add("requests", requests);
            for (JsonElement e : sdk.batchEmbedContents(model, body).getAsJsonArray("embeddings")) {
                JsonArray values = e.getAsJsonObject().getAsJsonArray("values");
                float[] v = new float[values.size()];
                for (int i = 0; i < v.length; i++) v[i] = values.get(i).getAsFloat();
                out.add(v);
            }
        }
        return out;
    }

    @Override public String modelName() { return "gemini:" + model; }
    @Override public double suggestedMinScore() { return 0.55; }
}
