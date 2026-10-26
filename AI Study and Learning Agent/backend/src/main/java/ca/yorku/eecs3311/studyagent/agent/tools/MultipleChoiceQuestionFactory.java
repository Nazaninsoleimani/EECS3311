package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.MultipleChoiceQuestion;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionSpec;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.util.List;
import java.util.Locale;

public class MultipleChoiceQuestionFactory extends QuestionFactory {
    @Override
    protected Question createQuestion(QuestionSpec spec, String id) {
        List<String> options = spec.options == null ? List.of()
                : spec.options.stream().filter(o -> o != null && !o.isBlank()).map(String::trim)
                        .map(o -> o.replaceFirst("^[A-Ea-e][).:]\\s+", "")).toList();
        if (options.size() < 3 || options.size() > 5) throw new ValidationException("MCQ needs 3-5 options, got " + options.size());
        if (options.stream().map(o -> o.toLowerCase(Locale.ROOT)).distinct().count() != options.size()) {
            throw new ValidationException("MCQ options must be distinct");
        }
        int correct = correctIndex(spec.answer, options);
        return new MultipleChoiceQuestion(id, spec.prompt, spec.difficulty, spec.conceptId, spec.conceptName, options, correct);
    }

    static int correctIndex(String answer, List<String> options) {
        if (answer == null || answer.isBlank()) throw new ValidationException("MCQ has no correct answer");
        String a = answer.trim();
        if (a.matches("(?i)[A-E]([).:].*)?")) {
            int idx = Character.toUpperCase(a.charAt(0)) - 'A';
            if (idx < options.size()) return idx;
        }
        if (a.matches("\\d+")) {
            int n = Integer.parseInt(a);
            if (n >= 0 && n < options.size()) return n;
        }
        for (int i = 0; i < options.size(); i++) if (options.get(i).equalsIgnoreCase(a)) return i;
        throw new ValidationException("MCQ answer '" + a + "' does not match an option");
    }
}
