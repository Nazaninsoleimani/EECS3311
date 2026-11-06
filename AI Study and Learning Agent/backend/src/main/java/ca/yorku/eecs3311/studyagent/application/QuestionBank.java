package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.AdaptiveLearningSession;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.persistence.QuizRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * F12 fallback when the LLM cannot generate the next adaptive activity: reuse a question generated
 * earlier for the course. Questions at the session's level and of a type its DifficultyState prefers
 * come first, then the same level, then any; within a group the least-mastered concept wins.
 */
public class QuestionBank {
    private final QuizRepository quizzes;

    public QuestionBank(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    /** A stored question not yet asked in this session, or null when none is left. */
    public Question pick(AdaptiveLearningSession session, Map<String, Double> masteryByConcept) {
        Map<String, Question> unique = new LinkedHashMap<>();
        for (Quiz quiz : quizzes.findByCourse(session.getCourseId())) {
            for (Question q : quiz.getQuestions()) {
                if (!session.getAskedQuestionIds().contains(q.getId())) unique.putIfAbsent(q.getPrompt().toLowerCase(), q);
            }
        }
        return unique.values().stream()
                .min(Comparator.comparingInt((Question q) -> rank(session, q))
                        .thenComparingDouble(q -> masteryByConcept.getOrDefault(q.getConceptId(), 0.0)))
                .orElse(null);
    }

    /** 0 = right level and preferred type, 1 = right level, 2 = anything else. */
    static int rank(AdaptiveLearningSession session, Question q) {
        boolean level = q.getDifficulty() == session.getLevel();
        boolean type = session.getState().preferredQuestionTypes().contains(q.getType());
        return level && type ? 0 : level ? 1 : 2;
    }
}
