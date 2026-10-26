package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

/** Raw question description produced by the LLM, before factory validation. */
public class QuestionSpec {
    public QuestionType type;
    public String prompt;
    public List<String> options;
    /** MCQ: letter, index or option text; TF: "true"/"false"; short: model answer. */
    public String answer;
    public List<String> rubric;
    public String conceptName;
    public DifficultyLevel difficulty;
}
