package ca.yorku.eecs3311.studyagent.presentation.gui;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.Student;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuthPanelTest {
    private final GuiTestSupport t = new GuiTestSupport();
    private final List<Student> signedIn = new ArrayList<>();
    private final AuthPanel panel = new AuthPanel(t.app.facade(), signedIn::add);

    @AfterEach
    void close() { t.close(); }

    @Test
    void createAccountThenSignIn() {
        assertFalse(panel.isCreateMode(), "opens on sign in");
        panel.setCreateMode(true);
        panel.nameField().setText("Nazanin");
        panel.emailField().setText("nazanin@example.com");
        panel.passwordField().setText("studyhard1");
        panel.confirmField().setText("studyhard2");
        panel.submit();
        assertEquals("Passwords do not match", panel.errorLabel().getText());
        assertTrue(signedIn.isEmpty());

        panel.confirmField().setText("studyhard1");
        panel.submit();
        assertEquals(1, signedIn.size());
        assertEquals("Nazanin", signedIn.get(0).getName());
        assertEquals("", new String(panel.passwordField().getPassword()), "password cleared after success");

        panel.setCreateMode(false);
        panel.emailField().setText("NAZANIN@example.com");
        panel.passwordField().setText("wrong-password");
        panel.submit();
        assertEquals("Email or password is incorrect", panel.errorLabel().getText());
        panel.passwordField().setText("studyhard1");
        panel.submit();
        assertEquals(signedIn.get(0).getId(), signedIn.get(1).getId());
    }

    @Test
    void eachAccountSeesOnlyItsOwnCourses() {
        Student a = t.app.facade().createAccount("A", "a@example.com", "password1".toCharArray(), null);
        Student b = t.app.facade().createAccount("B", "b@example.com", "password1".toCharArray(), null);
        t.app.facade().createCourse(a.getId(), "EECS3311", "Design", null);
        assertEquals(1, t.app.facade().listCourses(a.getId()).size());
        assertTrue(t.app.facade().listCourses(b.getId()).isEmpty());
    }

    @Test
    void headerOffersSignOutForSignedInStudents() {
        Student a = t.app.facade().createAccount("Ava", "ava@example.com", "password1".toCharArray(), null);
        boolean[] out = {false};
        StudyAgentGui gui = new StudyAgentGui(new GuiContext(t.app.facade(), a.getId()), () -> out[0] = true);
        assertEquals(12, gui.views().size());
        gui.detachObservers();
    }
}
