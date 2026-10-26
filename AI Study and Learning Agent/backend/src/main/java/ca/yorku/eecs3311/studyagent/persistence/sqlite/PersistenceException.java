package ca.yorku.eecs3311.studyagent.persistence.sqlite;

public class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
