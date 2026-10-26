package ca.yorku.eecs3311.studyagent.agent;

/** One prompt template (in resources/prompts) per kind of LLM task. */
public enum PromptTask {
    ANSWER_WITH_CITATIONS("answer-with-citations"),
    REWRITE_QUERY("rewrite-query"),
    SUMMARIZE_SECTION("summarize-section"),
    SUMMARIZE_REDUCE("summarize-reduce"),
    EXTRACT_CONCEPTS("extract-concepts"),
    PRIORITIZE_TOPICS("prioritize-topics"),
    GENERATE_QUIZ("generate-quiz"),
    GENERATE_FLASHCARDS("generate-flashcards"),
    GRADE_SHORT_ANSWER("grade-short-answer"),
    EXPLAIN_ANSWER("explain-answer"),
    DIAGNOSE_WEAKNESS("diagnose-weakness");

    public final String template;

    PromptTask(String template) {
        this.template = template;
    }
}
