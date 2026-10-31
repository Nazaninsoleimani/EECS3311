package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.SummaryLength;
import ca.yorku.eecs3311.studyagent.domain.SummaryOptions;
import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
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
    private final JTextArea output = readOnlyArea();
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
        top.add(row(button("Upload file...", this::chooseAndUpload), button("Delete", this::delete),
                new JLabel("   Summary length"), length, button("Summarize", this::summarize),
                button("Extract concepts", this::extractConcepts)), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(table), scroll(output));
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
        if (c == null) output.setText("Select a course to see its materials.");
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
        runAsync("Uploading and indexing " + path.getFileName(), () -> facade.uploadMaterial(c.getId(), path), m -> {
            render();
            output.setText(m.isIndexed() ? "Uploaded " + m.getTitle() + ": " + m.getChunkCount() + " chunks indexed for retrieval."
                    : "Could not index " + m.getTitle() + ": " + m.getFailureReason());
        });
    }

    private void delete() {
        CourseMaterial m = selected(true);
        if (m == null || !confirm("Delete " + m.getTitle() + " and its index?")) return;
        attempt(() -> {
            facade.deleteMaterial(m.getId());
            render();
            output.setText("Deleted " + m.getTitle());
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
        StringBuilder sb = new StringBuilder(s.getLength() + " SUMMARY\n\n").append(s.getText()).append("\n\nKey points:\n");
        s.getKeyPoints().forEach(k -> sb.append("  • ").append(k).append('\n'));
        if (!s.getSourceRefs().isEmpty()) sb.append("\nSources: ").append(String.join(", ", s.getSourceRefs()));
        output.setText(sb.toString());
        output.setCaretPosition(0);
    }

    private void extractConcepts() {
        CourseMaterial m = selected(true);
        if (m == null) return;
        runAsync("Extracting concepts (AI)", () -> facade.extractConcepts(m.getId()), g -> {
            output.setText(describe(g, m.getTitle()));
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

    JTextArea output() { return output; }
    JTable table() { return table; }
}
