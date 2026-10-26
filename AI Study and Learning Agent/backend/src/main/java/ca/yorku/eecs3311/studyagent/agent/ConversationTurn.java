package ca.yorku.eecs3311.studyagent.agent;

import java.time.LocalDateTime;

public class ConversationTurn {
    public final String question;
    public final String answer;
    public final LocalDateTime timestamp;

    public ConversationTurn(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.timestamp = LocalDateTime.now();
    }
}
