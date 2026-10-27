package ca.yorku.eecs3311.studyagent.agent;

/** A goal the agent could not complete; the message is shown to the student. */
public class AgentException extends RuntimeException {
    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
