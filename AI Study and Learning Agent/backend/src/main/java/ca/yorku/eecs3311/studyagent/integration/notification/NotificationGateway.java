package ca.yorku.eecs3311.studyagent.integration.notification;

public interface NotificationGateway {
    /** Returns false if delivery failed; the reminder then stays pending and is retried. */
    boolean send(String title, String body, String recipient);
}
