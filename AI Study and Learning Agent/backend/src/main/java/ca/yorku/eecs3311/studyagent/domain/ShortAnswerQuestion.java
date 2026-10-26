package ca.yorku.eecs3311.studyagent.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Not objective: GradingService normally scores it with the LLM against the rubric.
 * grade() is the deterministic fallback (rubric keyword coverage) and is flagged needsReview.
 */
public class ShortAnswerQuestion extends Question {
    private final String modelAnswer;
    private final List<String> rubric;

    public ShortAnswerQuestion(String id, String prompt, DifficultyLevel difficulty, String conceptId,
                               String conceptName, String modelAnswer, List<String> rubric) {
        super(id, prompt, difficulty, conceptId, conceptName);
        if (modelAnswer == null || modelAnswer.isBlank()) throw new ValidationException("Short answer needs a model answer");
        this.modelAnswer = modelAnswer.trim();
        this.rubric = rubric == null || rubric.isEmpty() ? List.of(this.modelAnswer) : List.copyOf(rubric);
    }

    @Override
    public GradeResult grade(QuestionResponse response) {
        if (isBlank(response)) return unanswered();
        Set<String> answerWords = words(response.answerText);
        long covered = rubric.stream().filter(point -> coverage(words(point), answerWords) >= 0.5).count();
        double score = (double) covered / rubric.size();
        GradeResult result = new GradeResult(id, conceptId, score >= 0.6, score,
                String.format("Covered %d of %d rubric points (keyword match). Model answer: %s", covered, rubric.size(), modelAnswer));
        result.needsReview = true;
        return result;
    }

    private static double coverage(Set<String> pointWords, Set<String> answerWords) {
        if (pointWords.isEmpty()) return 0;
        long hit = pointWords.stream().filter(answerWords::contains).count();
        return (double) hit / pointWords.size();
    }

    private static Set<String> words(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override public boolean isObjective() { return false; }
    @Override public QuestionType getType() { return QuestionType.SHORT_ANSWER; }
    @Override public String correctAnswerText() { return modelAnswer; }

    public String getModelAnswer() { return modelAnswer; }
    public List<String> getRubric() { return rubric; }
}
