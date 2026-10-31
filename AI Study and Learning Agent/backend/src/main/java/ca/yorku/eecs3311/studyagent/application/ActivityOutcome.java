package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.DifficultyLevel;
import ca.yorku.eecs3311.studyagent.domain.GradeResult;
import ca.yorku.eecs3311.studyagent.domain.Question;

/**
 * Result of one adaptive-session answer: the grade, the difficulty level after the State transition,
 * and the next question (null if the agent could not produce one).
 */
public record ActivityOutcome(GradeResult result, DifficultyLevel previousLevel, DifficultyLevel level, Question next, String notice) {
    public boolean levelChanged() { return previousLevel != level; }
}
