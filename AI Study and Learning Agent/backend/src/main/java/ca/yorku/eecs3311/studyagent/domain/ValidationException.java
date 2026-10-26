package ca.yorku.eecs3311.studyagent.domain;

/** Raised when user input or generated content violates a domain rule. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
