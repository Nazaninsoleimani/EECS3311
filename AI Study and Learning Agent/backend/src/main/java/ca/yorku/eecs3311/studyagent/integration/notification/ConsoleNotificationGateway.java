package ca.yorku.eecs3311.studyagent.integration.notification;

import java.io.PrintStream;

/** Used by the CLI: due reminders are printed in the terminal. */
public class ConsoleNotificationGateway implements NotificationGateway {
    private final PrintStream out;

    public ConsoleNotificationGateway(PrintStream out) {
        this.out = out;
    }

    @Override
    public boolean send(String title, String body, String recipient) {
        out.println("⏰ " + title + ": " + body);
        return true;
    }
}
