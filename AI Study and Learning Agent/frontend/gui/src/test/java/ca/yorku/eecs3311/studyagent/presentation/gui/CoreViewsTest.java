package ca.yorku.eecs3311.studyagent.presentation.gui;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Builds the views headlessly and checks they show the facade's data. */
class CoreViewsTest {
    @TempDir Path dir;
    private final GuiTestSupport t = new GuiTestSupport();

    @AfterEach
    void close() { t.close(); }

    @Test
    void courseListAndSelectionStayInSync() {
        CourseView view = new CourseView(t.ctx);
        List<Course> seen = new java.util.ArrayList<>();
        t.ctx.onCourseChanged(seen::add);
        Course a = t.app.facade().createCourse(t.app.studentId(), "EECS3311", "Design", null);
        t.app.facade().createCourse(t.app.studentId(), "EECS3101", "Algorithms", null);
        t.ctx.reloadCourses();
        assertEquals(2, view.table().getRowCount());
        assertEquals(a.getId(), t.ctx.course().getId(), "first course selected by default");
        view.table().getSelectionModel().setSelectionInterval(1, 1);
        assertEquals("EECS3101", t.ctx.course().getCode());
        assertEquals("EECS3101", view.codeField().getText());
        assertEquals(2, seen.size());
        t.app.facade().deleteCourse(t.ctx.course().getId());
        t.ctx.reloadCourses();
        assertEquals("EECS3311", t.ctx.course().getCode(), "selection falls back to a remaining course");
    }

    @Test
    void materialViewListsMaterialsAndShowsSummaries() throws Exception {
        t.courseWithMaterial(dir);
        MaterialView view = new MaterialView(t.ctx);
        view.render();
        assertEquals(1, view.table().getRowCount());
        assertEquals(MaterialStatus.INDEXED, view.table().getValueAt(0, 2));
        String id = t.app.facade().listMaterials(t.ctx.course().getId()).get(0).getId();
        view.displaySummary(t.app.facade().summarizeMaterial(id, new SummaryOptions(SummaryLength.BRIEF)));
        assertTrue(view.output().getText().contains("• Subjects notify"));
        assertTrue(MaterialView.describe(t.app.facade().getConceptMap(t.app.studentId(), t.ctx.course().getId()), "x")
                .contains("Observer Pattern - subjects notify observers"));
    }

    @Test
    void tutorShowsAnswerWithSources() throws Exception {
        Course c = t.courseWithMaterial(dir);
        TutorChatView view = new TutorChatView(t.ctx);
        view.render();
        assertTrue(view.transcript().getText().contains("Ask anything about EECS3311"));
        view.displayAnswer(t.app.facade().askQuestion("s", c.getId(), "What does a subject do in the observer pattern?"));
        assertTrue(view.transcript().getText().contains("Sources: [1] patterns.md"));
    }

    @Test
    void mainWindowHasATabPerView() {
        StudyAgentGui gui = new StudyAgentGui(t.ctx);
        List<String> titles = gui.views().stream().map(BaseView::title).toList();
        assertTrue(titles.containsAll(List.of("Courses", "Materials", "Tutor")), titles.toString());
    }
}
