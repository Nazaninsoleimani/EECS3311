package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import ca.yorku.eecs3311.studyagent.persistence.ConceptRepository;
import ca.yorku.eecs3311.studyagent.persistence.ProgressRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic topic ranking (F11): priority = 0.6 (1 - mastery) + 0.25 recency gap + 0.15 prerequisite weight.
 * Unpractised concepts count as mastery 0 and maximal recency gap.
 */
public class ProgressAnalysisTool implements Tool {
    private final ConceptRepository concepts;
    private final ProgressRepository progress;

    public ProgressAnalysisTool(ConceptRepository concepts, ProgressRepository progress) {
        this.concepts = concepts;
        this.progress = progress;
    }

    @Override public String getName() { return "progress_analysis"; }
    @Override public String getDescription() { return "Rank course concepts by weakness, time since practice and prerequisite importance"; }

    @Override
    public ToolResult execute(ToolInput input) {
        String studentId = input.getString("studentId");
        String courseId = input.getString("courseId");
        List<Concept> all = concepts.findByCourse(courseId);
        if (all.isEmpty()) return ToolResult.fail("This course has no concepts yet; upload material first");
        ConceptGraph graph = new ConceptGraph(all, concepts.findRelations(courseId));
        Map<String, ConceptMastery> mastery = new HashMap<>();
        for (ConceptMastery m : progress.findMastery(studentId, courseId)) mastery.put(m.getConceptId(), m);
        return ToolResult.ok(rankTopics(graph, mastery, LocalDateTime.now()), all.size() + " concepts ranked");
    }

    public static List<TopicPriority> rankTopics(ConceptGraph graph, Map<String, ConceptMastery> mastery, LocalDateTime now) {
        int maxDependents = graph.nodes.stream().mapToInt(c -> graph.dependentsCount(c.getId())).max().orElse(0);
        List<TopicPriority> ranked = new ArrayList<>();
        for (Concept c : graph.nodes) {
            ConceptMastery m = mastery.get(c.getId());
            double level = m == null || m.getAttempts() == 0 ? 0.0 : m.getMastery();
            double recency = m == null || m.getLastPracticed() == null ? 1.0
                    : Math.min(1.0, Duration.between(m.getLastPracticed(), now).toHours() / (14.0 * 24));
            double prereq = maxDependents == 0 ? 0.0 : (double) graph.dependentsCount(c.getId()) / maxDependents;
            double priority = 0.6 * (1 - level) + 0.25 * recency + 0.15 * prereq;
            String reason = m == null || m.getAttempts() == 0 ? "not practised yet"
                    : String.format("mastery %.0f%% after %d attempt(s)", level * 100, m.getAttempts());
            if (prereq > 0) reason += "; prerequisite for " + graph.dependentsCount(c.getId()) + " concept(s)";
            ranked.add(new TopicPriority(c, priority, reason));
        }
        ranked.sort(Comparator.comparingDouble((TopicPriority t) -> t.priority).reversed());
        return ranked;
    }
}
