package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

public class MultipleChoiceQuestion extends Question {
    private final List<String> options;
    private final int correctIndex;

    public MultipleChoiceQuestion(String id, String prompt, DifficultyLevel difficulty, String conceptId,
                                  String conceptName, List<String> options, int correctIndex) {
        super(id, prompt, difficulty, conceptId, conceptName);
        if (options == null || options.size() < 2) throw new ValidationException("MCQ needs at least two options");
        if (correctIndex < 0 || correctIndex >= options.size()) throw new ValidationException("MCQ correct index out of range");
        this.options = List.copyOf(options);
        this.correctIndex = correctIndex;
    }

    /** Accepts a letter (A, b), a 1-based number, or the exact option text. Returns -1 if unrecognised. */
    public int parseChoice(String answer) {
        String a = answer.trim();
        if (a.length() == 1 && Character.isLetter(a.charAt(0))) {
            int idx = Character.toUpperCase(a.charAt(0)) - 'A';
            return idx < options.size() ? idx : -1;
        }
        try {
            int n = Integer.parseInt(a);
            return n >= 1 && n <= options.size() ? n - 1 : -1;
        } catch (NumberFormatException ignored) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equalsIgnoreCase(a)) return i;
            }
            return -1;
        }
    }

    @Override
    public GradeResult grade(QuestionResponse response) {
        if (isBlank(response)) return unanswered();
        boolean correct = parseChoice(response.answerText) == correctIndex;
        return new GradeResult(id, conceptId, correct, correct ? 1.0 : 0.0,
                correct ? "Correct." : "Incorrect. Correct answer: " + correctAnswerText());
    }

    @Override public boolean isObjective() { return true; }
    @Override public QuestionType getType() { return QuestionType.MULTIPLE_CHOICE; }
    @Override public String correctAnswerText() { return (char) ('A' + correctIndex) + ") " + options.get(correctIndex); }

    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
}
