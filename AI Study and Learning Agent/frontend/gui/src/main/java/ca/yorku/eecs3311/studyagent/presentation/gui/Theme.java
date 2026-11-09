package ca.yorku.eecs3311.studyagent.presentation.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.UIManager;

/**
 * Look and feel of the GUI: FlatLaf with one accent colour, rounded controls and a light/dark
 * switch remembered between runs. Colour helpers fall back to sensible values when FlatLaf is not
 * installed (headless tests), so views can always ask for them.
 */
final class Theme {
    static final Color ACCENT = new Color(0x7C3AED);
    static final Color SUCCESS = new Color(0x2E9E5B);
    static final Color WARNING = new Color(0xD99A00);
    static final Color DANGER = new Color(0xD64545);
    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);

    private Theme() {}

    static boolean isDark() {
        try {
            return PREFS.getBoolean("dark", false);
        } catch (RuntimeException e) {
            return false;
        }
    }

    static void install(boolean dark) {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#7C3AED"));
        if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TableHeader.height", 32);
        UIManager.put("TabbedPane.tabHeight", 42);
        UIManager.put("TabbedPane.selectedBackground", withAlpha(ACCENT, dark ? 60 : 30));
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.tabSelectionHeight", 3);
        UIManager.put("Component.focusWidth", 1);
    }

    /** Switches between light and dark, remembers the choice and repaints every open window. */
    static void toggle() {
        boolean dark = !isDark();
        try {
            PREFS.putBoolean("dark", dark);
        } catch (RuntimeException ignored) {
            // the choice just is not remembered
        }
        install(dark);
        FlatLaf.updateUI();
    }

    static Color background() { return color("Panel.background", new Color(0xF6F7FB)); }

    /** Cards are slightly lighter than the background in light mode and slightly darker in dark mode. */
    static Color card() {
        Color bg = background();
        return isDarkColor(bg) ? shift(bg, 12) : Color.WHITE;
    }

    static Color border() { return color("Component.borderColor", new Color(0xDADCE3)); }

    static Color text() { return color("Label.foreground", new Color(0x1B1F24)); }

    static Color muted() { return color("Label.disabledForeground", new Color(0x6E7781)); }

    static boolean isDarkColor(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 < 128;
    }

    static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Opaque blend of c over the card colour, for tinted backgrounds that HTML can render. */
    static Color tint(Color c, double amount) {
        Color base = card();
        return new Color((int) (base.getRed() + (c.getRed() - base.getRed()) * amount),
                (int) (base.getGreen() + (c.getGreen() - base.getGreen()) * amount),
                (int) (base.getBlue() + (c.getBlue() - base.getBlue()) * amount));
    }

    static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Colour for a mastery value: red below 50%, amber below 80%, green otherwise. */
    static Color forMastery(double mastery) {
        return mastery < 0.5 ? DANGER : mastery < 0.8 ? WARNING : SUCCESS;
    }

    private static Color shift(Color c, int d) {
        return new Color(Math.min(255, c.getRed() + d), Math.min(255, c.getGreen() + d), Math.min(255, c.getBlue() + d));
    }

    private static Color color(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c == null ? fallback : c;
    }
}
