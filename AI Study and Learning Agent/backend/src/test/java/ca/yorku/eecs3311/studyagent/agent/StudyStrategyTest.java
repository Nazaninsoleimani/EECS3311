package ca.yorku.eecs3311.studyagent.agent;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.agent.tools.*;
import ca.yorku.eecs3311.studyagent.domain.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StudyStrategyTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    private static List<TopicPriority> topics(int n) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> new TopicPriority(new Concept("k" + i, "c1", "Topic " + i, ""), 1.0 - i * 0.1, ""))
                .toList();
    }

    private static StudyPlanRequest request(int days, int minutes) {
        return new StudyPlanRequest("s1", "c1", START.plusDays(days), minutes, List.of(), START);
    }

    @Test
    void strategySelectedByTimeLeft() {
        StudyScheduleTool tool = new StudyScheduleTool();
        assertInstanceOf(ExamCramStrategy.class, tool.selectStrategy(request(4, 60)));
        assertInstanceOf(BalancedReviewStrategy.class, tool.selectStrategy(request(14, 60)));
        assertInstanceOf(SpacedRepetitionStrategy.class, tool.selectStrategy(request(42, 60)));
        assertInstanceOf(BalancedReviewStrategy.class,
                tool.selectStrategy(new StudyPlanRequest("s1", "c1", null, 60, List.of(), START)));
    }

    @Test
    void cramFillsEveryDayAndEndsWithReview() {
        List<StudyTask> tasks = new ExamCramStrategy().buildSchedule(request(4, 90), topics(10));
        assertEquals(4 * 3, tasks.size());
        assertTrue(tasks.stream().allMatch(t -> t.getDurationMinutes() == 30));
        assertTrue(tasks.stream().filter(t -> t.getDate().equals(START.plusDays(3)))
                .allMatch(t -> t.getActivity() == ActivityType.REVIEW));
        assertEquals("k0", tasks.get(0).getConceptId());
        assertTrue(tasks.stream().noneMatch(t -> t.getDate().isAfter(START.plusDays(3))));
    }

    @Test
    void spacedRepetitionRevisitsTopicsAtGrowingIntervals() {
        List<StudyTask> tasks = new SpacedRepetitionStrategy().buildSchedule(request(30, 60), topics(3));
        List<LocalDate> topic0 = tasks.stream().filter(t -> t.getConceptId().equals("k0")).map(StudyTask::getDate).toList();
        assertEquals(List.of(START, START.plusDays(1), START.plusDays(3), START.plusDays(7), START.plusDays(14)), topic0);
        assertEquals(ActivityType.READ, tasks.get(0).getActivity());
        Map<LocalDate, Integer> perDay = new HashMap<>();
        tasks.forEach(t -> perDay.merge(t.getDate(), 1, Integer::sum));
        assertTrue(perDay.values().stream().allMatch(n -> n <= 2), "never more than slotsPerDay");
    }

    @Test
    void scheduleToolReportsUnscheduledTopics() {
        StudyScheduleTool tool = new StudyScheduleTool();
        ToolResult r = tool.execute(ToolInput.of("request", request(2, 30), "priorities", topics(6)));
        StudyScheduleTool.Schedule s = r.payload();
        assertEquals("Exam cram", s.strategyName());
        assertFalse(s.unscheduledTopics().isEmpty());
        assertTrue(s.unscheduledTopics().contains("Topic 5"));
    }

    @Test
    void progressRankingFavoursWeakAndPrerequisiteConcepts() {
        Concept a = new Concept("a", "c1", "Classes", "");
        Concept b = new Concept("b", "c1", "Inheritance", "");
        Concept c = new Concept("c", "c1", "Polymorphism", "");
        ConceptGraph g = new ConceptGraph(List.of(a, b, c), List.of(new ConceptRelation("c1", "b", "c", RelationType.PREREQUISITE_OF)));
        Map<String, ConceptMastery> m = new HashMap<>();
        ConceptMastery strong = new ConceptMastery("s1", "c1", "a", "Classes");
        strong.applyResult(new GradeResult("q", "a", true, 1.0, ""));
        ConceptMastery weak = new ConceptMastery("s1", "c1", "b", "Inheritance");
        weak.applyResult(new GradeResult("q", "b", false, 0.2, ""));
        m.put("a", strong);
        m.put("b", weak);
        List<TopicPriority> ranked = ProgressAnalysisTool.rankTopics(g, m, LocalDateTime.now());
        assertEquals("c", ranked.get(0).concept.getId(), "never practised ranks first");
        assertEquals("b", ranked.get(1).concept.getId());
        assertEquals("a", ranked.get(2).concept.getId());
        assertTrue(ranked.get(1).reason.contains("prerequisite"));
    }
}
