package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import java.util.List;

/** Strategy pattern: interchangeable algorithms for distributing study time. */
public interface StudyPlanningStrategy {
    String getName();

    /** Priorities are ordered most urgent first. */
    List<StudyTask> buildSchedule(StudyPlanRequest request, List<TopicPriority> priorities);
}
