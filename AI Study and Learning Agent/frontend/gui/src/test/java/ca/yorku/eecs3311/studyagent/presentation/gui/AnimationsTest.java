package ca.yorku.eecs3311.studyagent.presentation.gui;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import org.junit.jupiter.api.Test;

class AnimationsTest {
    @Test
    void easingStartsAtZeroEndsAtOneAndNeverOvershoots() {
        assertEquals(0.0, Animations.easeOut(0.0), 1e-9);
        assertEquals(1.0, Animations.easeOut(1.0), 1e-9);
        for (double t = 0; t <= 1; t += 0.05) {
            double v = Animations.easeOut(t);
            assertTrue(v >= 0 && v <= 1);
            assertTrue(v >= t - 1e-9, "ease-out is ahead of linear");
        }
    }

    @Test
    void withoutADisplayAnimationsJumpToTheirFinalState() {
        JLabel percent = new JLabel("67%");
        Animations.countUp(percent, 700);
        assertEquals("67%", percent.getText());
        JLabel text = new JLabel("Done");
        Animations.countUp(text, 700);
        assertEquals("Done", text.getText(), "non-numbers are left alone");
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(40);
        Animations.fill(bar, 800);
        assertEquals(40, bar.getValue());
        assertEquals("40%", bar.getString());
        boolean[] changed = {false};
        Animations.crossFade(null, () -> changed[0] = true, 300);
        assertTrue(changed[0], "the change itself always happens");
    }
}
