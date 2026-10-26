package ca.yorku.eecs3311.studyagent.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Short-term memory of a tutor chat: the last maxTurns question/answer pairs. */
public class SessionMemory {
    private final String sessionId;
    private final Deque<ConversationTurn> turns = new ArrayDeque<>();
    private final int maxTurns;

    public SessionMemory(String sessionId, int maxTurns) {
        this.sessionId = sessionId;
        this.maxTurns = maxTurns;
    }

    public void add(ConversationTurn turn) {
        turns.addLast(turn);
        while (turns.size() > maxTurns) turns.removeFirst();
    }

    public List<ConversationTurn> recent(int n) {
        List<ConversationTurn> all = new ArrayList<>(turns);
        return all.subList(Math.max(0, all.size() - n), all.size());
    }

    public boolean isEmpty() { return turns.isEmpty(); }
    public String getSessionId() { return sessionId; }
}
