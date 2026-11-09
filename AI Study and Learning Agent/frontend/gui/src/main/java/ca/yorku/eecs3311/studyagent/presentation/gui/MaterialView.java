package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.application.MaterialUpload;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.SummaryLength;
import ca.yorku.eecs3311.studyagent.domain.SummaryOptions;
import java.awt.BorderLayout;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/** F02 upload, F03 summaries and F04 concept extraction for the selected course's materials. */
public class MaterialView extends BaseView {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Title", "Type", "Status", "Chunks / problem", "Uploaded"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<SummaryLength> length = new JComboBox<>(SummaryLength.values());
    private final JEditorPane output = new JEditorPane("text/html", "");
    /** Builds the reading pane's current page; rebuilt on render() so a theme switch restyles it. */
    private Supplier<String> page;
    private List<CourseMaterial> shown = List.of();

    public MaterialView(GuiContext ctx) {
        super(ctx);
        length.setSelectedItem(SummaryLength.STANDARD);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            CourseMaterial m = selected(false);
            if (!e.getValueIsAdjusting() && m != null && m.getSummary() != null) displaySummary(m.getSummary());
        });
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Course materials"), BorderLayout.NORTH);
        top.add(row(accentButton("Upload file...", this::chooseAndUpload), button("Delete", this::delete), button("Retry indexing", this::retryIndexing),
                new JLabel("   Summary length"), length, button("Summarize", this::summarize),
                button("Extract concepts", this::extractConcepts)), BorderLayout.SOUTH);
        output.setEditable(false);
        output.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        output.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) followLink(e.getDescription());
        });
        JScrollPane reading = new JScrollPane(output);
        reading.setBorder(BorderFactory.createLineBorder(Theme.border()));
        reading.getVerticalScrollBar().setUnitIncrement(16);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(table), reading);
        split.setResizeWeight(0.4);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    @Override public String title() { return "Materials"; }

    @Override
    public void render() {
        Course c = ctx.course();
        shown = c == null ? List.of() : facade.listMaterials(c.getId());
        model.setRowCount(0);
        for (CourseMaterial m : shown) {
            model.addRow(new Object[]{m.getTitle(), m.getFileType(), m.getStatus(),
                    m.isIndexed() ? m.getChunkCount() : m.getFailureReason(), m.getUploadedAt().toLocalDate()});
        }
        if (c == null || page == null) page = () -> StudyNotesHtml.welcome(c != null);
        refreshPage();
    }

    private void show(Supplier<String> newPage) {
        page = newPage;
        refreshPage();
    }

    private void refreshPage() {
        output.setText(page.get());
        output.setCaretPosition(0);
    }

    private void followLink(String target) {
        StudyNotesHtml.follow(ctx, target);
    }

    private void chooseAndUpload() {
        Course c = requireCourse();
        if (c == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Course material (PDF, DOCX, TXT, MD)", "pdf", "docx", "txt", "md", "markdown"));
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        for (File f : chooser.getSelectedFiles()) uploadFile(f.toPath());
    }

    public void uploadFile(Path path) {
        Course c = requireCourse();
        if (c == null) return;
        runAsync("Uploading " + path.getFileName(), () -> facade.storeMaterial(c.getId(), path), m -> {
            render();
            if (m.isIndexingPending()) {
                show(() -> StudyNotesHtml.message(StudyNotesHtml.Kind.WARNING, "Saved " + m.getTitle() + ", indexing pending",
                        m.getFailureReason() + " Use Retry indexing later."));
                return;
            }
            if (!m.isIndexed()) {
                show(() -> StudyNotesHtml.message(StudyNotesHtml.Kind.ERROR, "Could not index " + m.getTitle(), m.getFailureReason()));
                return;
            }
            String done = "Uploaded " + m.getTitle();
            String detail = m.getChunkCount() + " sections indexed - the tutor and quizzes can use it now.";
            if (!facade.isLlmAvailable()) {
                show(() -> StudyNotesHtml.messages(new String[]{"SUCCESS", done, detail},
                        new String[]{"WARNING", "Concepts were not extracted", "No AI model is configured."}));
                return;
            }
            show(() -> StudyNotesHtml.messages(new String[]{"SUCCESS", done, detail},
                    new String[]{"INFO", "Extracting key concepts in the background...", "You can keep working; they will appear here."}));
            // Concept extraction calls the AI and can take a while, so it runs after the upload has finished.
            runAsync("Extracting concepts from " + m.getTitle(), () -> facade.extractConcepts(m.getId()), g -> {
                show(() -> StudyNotesHtml.concepts(g, m.getTitle(), done + " - " + detail));
                ctx.fireDataChanged();
            });
        });
    }

    static String describe(MaterialUpload up) {
        CourseMaterial m = up.material();
        if (m.isIndexingPending()) return "Saved " + m.getTitle() + ", but " + m.getFailureReason() + "\nUse Retry indexing later.";
        if (!m.isIndexed()) return "Could not index " + m.getTitle() + ": " + m.getFailureReason();
        String head = "Uploaded " + m.getTitle() + ": " + m.getChunkCount() + " chunks indexed for retrieval.\n\n";
        return head + (up.conceptsExtracted() ? describe(up.concepts(), m.getTitle()) : "Note: " + up.conceptWarning());
    }

    private void retryIndexing() {
        runAsync("Retrying indexing", () -> facade.retryPendingIndexing(ctx.studentId), n -> {
            render();
            show(() -> StudyNotesHtml.message(n > 0 ? StudyNotesHtml.Kind.SUCCESS : StudyNotesHtml.Kind.INFO,
                    n + " material(s) indexed", n > 0 ? "They are ready for summaries, the tutor and quizzes." : "Nothing was waiting for indexing."));
        });
    }

    private void delete() {
        CourseMaterial m = selected(true);
        if (m == null || !confirm("Delete " + m.getTitle() + " and its index?")) return;
        attempt(() -> {
            facade.deleteMaterial(m.getId());
            render();
            show(() -> StudyNotesHtml.message(StudyNotesHtml.Kind.INFO, "Deleted " + m.getTitle(), "Its index was removed as well."));
        });
    }

    public void requestSummary(String materialId, SummaryLength len) {
        runAsync("Summarizing (AI)", () -> facade.summarizeMaterial(materialId, new SummaryOptions(len)), s -> {
            displaySummary(s);
            render();
        });
    }

    private void summarize() {
        CourseMaterial m = selected(true);
        if (m != null) requestSummary(m.getId(), (SummaryLength) length.getSelectedItem());
    }

    public void displaySummary(Summary s) {
        String title = shown.stream().filter(m -> m.getId().equals(s.getMaterialId())).map(CourseMaterial::getTitle)
                .findFirst().orElseGet(() -> facade.listMaterials(ctx.course() == null ? "" : ctx.course().getId()).stream()
                        .filter(m -> m.getId().equals(s.getMaterialId())).map(CourseMaterial::getTitle).findFirst().orElse("Summary"));
        show(() -> StudyNotesHtml.summary(s, title));
    }

    private void extractConcepts() {
        CourseMaterial m = selected(true);
        if (m == null) return;
        runAsync("Extracting concepts (AI)", () -> facade.extractConcepts(m.getId()), g -> {
            show(() -> StudyNotesHtml.concepts(g, m.getTitle(), null));
            ctx.fireDataChanged();
        });
    }

    static String describe(ConceptGraph g, String title) {
        StringBuilder sb = new StringBuilder(g.nodes.size() + " concept(s) and " + g.edges.size() + " relation(s) from " + title + ":\n\n");
        g.nodes.forEach(c -> sb.append("• ").append(c.getName()).append(c.getDefinition().isBlank() ? "" : " - " + c.getDefinition()).append('\n'));
        sb.append("\nSee the Concept Map tab for the relations.");
        return sb.toString();
    }

    private CourseMaterial selected(boolean required) {
        int i = table.getSelectedRow();
        if (i < 0 || i >= shown.size()) {
            if (required) showError("Select a material in the table first.");
            return null;
        }
        return shown.get(i);
    }

    JEditorPane output() { return output; }

    /** The reading pane as plain text (for tests). */
    String outputText() {
        try {
            return output.getDocument().getText(0, output.getDocument().getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }
    JTable table() { return table; }
}
