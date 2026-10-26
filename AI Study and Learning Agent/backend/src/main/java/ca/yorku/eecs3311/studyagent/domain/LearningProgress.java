package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

/** Course-level progress derived from mastery, performance records and the active plan. */
public class LearningProgress {
    public final List<ConceptMastery> masteries;
    public final double averageMastery;
    public final double quizAverage;
    public final double planCompletion;
    public final int conceptsMastered;
    public final int gradedActivities;

    public LearningProgress(List<ConceptMastery> masteries, List<PerformanceRecord> records, StudyPlan activePlan) {
        this.masteries = List.copyOf(masteries);
        List<ConceptMastery> practised = masteries.stream().filter(m -> m.getAttempts() > 0).toList();
        this.averageMastery = practised.stream().mapToDouble(ConceptMastery::getMastery).average().orElse(0.0);
        this.quizAverage = records.stream().filter(r -> r.source == ActivityType.QUIZ)
                .mapToDouble(r -> r.score).average().orElse(0.0);
        this.planCompletion = activePlan == null ? 0.0 : activePlan.completionRate();
        this.conceptsMastered = (int) masteries.stream().filter(ConceptMastery::isMastered).count();
        this.gradedActivities = records.size();
    }

    public boolean isEmpty() { return gradedActivities == 0; }
}
