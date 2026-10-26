package ca.yorku.eecs3311.studyagent.integration.notification;

import java.util.Locale;

/** Operating-system notification: macOS via osascript, otherwise the AWT system tray, else console. */
public class DesktopNotificationGateway implements NotificationGateway {
    @Override
    public boolean send(String title, String body, String recipient) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac")) {
                String script = "display notification \"" + escape(body) + "\" with title \"" + escape(title) + "\"";
                return new ProcessBuilder("osascript", "-e", script).start().waitFor() == 0;
            }
            if (java.awt.SystemTray.isSupported()) {
                java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
                java.awt.TrayIcon icon = new java.awt.TrayIcon(new java.awt.image.BufferedImage(16, 16,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB), "Study Agent");
                tray.add(icon);
                icon.displayMessage(title, body, java.awt.TrayIcon.MessageType.INFO);
                return true;
            }
        } catch (Exception e) {
            // fall through to console
        }
        System.out.println("[Reminder] " + title + ": " + body);
        return true;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
