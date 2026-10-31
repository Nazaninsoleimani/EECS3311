package ca.yorku.eecs3311.studyagent.presentation.cli;

import ca.yorku.eecs3311.studyagent.agent.AgentException;
import ca.yorku.eecs3311.studyagent.application.LearningFacade;
import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.config.AppContext;
import ca.yorku.eecs3311.studyagent.domain.Answer;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.SummaryLength;
import ca.yorku.eecs3311.studyagent.domain.SummaryOptions;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.notification.ConsoleNotificationGateway;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.PersistenceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command-line front end: "study <group> <action> [options]". Every command is a small handler
 * registered under its name; all of them call only the LearningFacade, like the GUI.
 * Without arguments it starts an interactive shell.
 */
public class CLIApplication {
    /** One CLI command: its usage line, a description for help, and the code that runs it. */
    record Command(String usage, String description, Handler handler) {}

    @FunctionalInterface
    interface Handler {
        String run(ParsedCommand cmd) throws IOException;
    }

    private final CommandParser parser = new CommandParser();
    private final Map<String, Command> commands = new LinkedHashMap<>();
    final LearningFacade facade;
    final String studentId;
    final BufferedReader in;
    final PrintStream out;

    public CLIApplication(LearningFacade facade, String studentId, BufferedReader in, PrintStream out) {
        this.facade = facade;
        this.studentId = studentId;
        this.in = in;
        this.out = out;
        registerCoreCommands();
        new StudyCommands(this).registerAll();
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try (AppContext app = AppContext.create(AppConfig.load(), new ConsoleNotificationGateway(out))) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            CLIApplication cli = new CLIApplication(app.facade(), app.studentId(), in, out);
            app.facade().dispatchDueReminders();
            int code = args.length == 0 ? cli.shell() : cli.run(args);
            if (code != 0) System.exit(code);
        }
    }

    void register(String name, String usage, String description, Handler handler) {
        commands.put(name, new Command(usage, description, handler));
    }

    /** Runs one command; returns the process exit code (0 ok, 1 failed, 2 wrong usage). */
    public int run(String[] args) {
        ParsedCommand cmd = parser.parse(args);
        Command c = commands.get(cmd.group + " " + cmd.action.toLowerCase(Locale.ROOT));
        if (c == null && commands.containsKey(cmd.group)) {
            c = commands.get(cmd.group);
            cmd = cmd.withActionAsArgument();
        }
        if (c == null) {
            print("Unknown command '" + (cmd.group + " " + cmd.action).trim() + "'. Run 'study help' for the list of commands.");
            return 2;
        }
        if (cmd.has("help")) {
            print("Usage: " + c.usage() + "\n  " + c.description());
            return 0;
        }
        try {
            print(dispatch(c, cmd));
            if (cmd.has("trace")) print("\n--- agent trace ---\n" + facade.getLastAgentTrace());
            return 0;
        } catch (ParsedCommand.UsageException e) {
            print("Error: " + e.getMessage() + "\nUsage: " + c.usage());
            return 2;
        } catch (ValidationException | AgentException | LLMException | PersistenceException e) {
            print("Error: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            print("Error: " + e.getMessage());
            return 1;
        }
    }

    private String dispatch(Command c, ParsedCommand cmd) throws IOException {
        return c.handler().run(cmd);
    }

    /** Interactive shell: type commands without the "study" prefix; "exit" leaves. */
    public int shell() {
        print("AI Study Agent - interactive mode. Type 'help' for commands, 'exit' to quit.\n" + facade.status());
        facade.startReminderTimer(60);
        while (true) {
            out.print("study> ");
            out.flush();
            String line = readLine();
            if (line == null || line.trim().equalsIgnoreCase("exit") || line.trim().equalsIgnoreCase("quit")) return 0;
            if (line.isBlank()) continue;
            String[] args = CommandParser.tokenize(line.trim().replaceFirst("^study\\s+", ""));
            run(args);
        }
    }

    void print(String text) {
        if (text != null && !text.isEmpty()) out.println(text);
    }

    String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Prints a label and reads one line; null at end of input. */
    String prompt(String label) {
        out.print(label);
        out.flush();
        return readLine();
    }

    Course course(ParsedCommand cmd) {
        return facade.resolveCourse(studentId, cmd.require("course"));
    }

    // ------------------------------------------------------------------ commands

    private void registerCoreCommands() {
        register("help", "study help", "List all commands", cmd -> help());
        register("status", "study status", "Show which LLM is configured and this run's usage", cmd -> facade.status());

        register("course create", "study course create --code <code> --title <title> [--exam yyyy-mm-dd]",
                "F01: create a course workspace", cmd -> "Created " + describe(facade.createCourse(studentId,
                        cmd.require("code"), cmd.require("title"), cmd.dateOption("exam"))));
        register("course list", "study course list", "F01: list your courses", cmd -> {
            List<Course> list = facade.listCourses(studentId);
            if (list.isEmpty()) return "No courses yet. Create one with: study course create --code EECS3311 --title \"Software Design\"";
            return list.stream().map(CLIApplication::describe).collect(Collectors.joining("\n"));
        });
        register("course update", "study course update <course> [--code c] [--title t] [--exam yyyy-mm-dd|none]",
                "F01: change a course's code, title or exam date", cmd -> {
                    Course c = facade.resolveCourse(studentId, cmd.arg(0, "course id or code"));
                    if (cmd.options.containsKey("code")) c.setCode(cmd.require("code"));
                    if (cmd.options.containsKey("title")) c.setTitle(cmd.require("title"));
                    if (cmd.options.containsKey("exam")) c.setExamDate("none".equalsIgnoreCase(cmd.options.get("exam")) ? null : cmd.dateOption("exam"));
                    return "Updated " + describe(facade.updateCourse(c));
                });
        register("course delete", "study course delete <course>", "F01: delete a course and all of its data", cmd -> {
            Course c = facade.resolveCourse(studentId, cmd.arg(0, "course id or code"));
            String answer = prompt("Delete " + c.getCode() + " and all its materials, quizzes and progress? [y/N] ");
            if (answer == null || !answer.trim().toLowerCase(Locale.ROOT).startsWith("y")) return "Cancelled.";
            facade.deleteCourse(c.getId());
            return "Deleted " + c.getCode() + ".";
        });

        register("material upload", "study material upload --course <course> <file>",
                "F02: upload a PDF, DOCX, TXT or Markdown file and index it", cmd -> {
                    Course c = course(cmd);
                    CourseMaterial m = facade.uploadMaterial(c.getId(), Path.of(cmd.arg(0, "file path")));
                    return m.isIndexed() ? "Uploaded " + m.getTitle() + " (id " + m.getId() + "): " + m.getChunkCount() + " chunks indexed."
                            : "Upload failed for " + m.getTitle() + ": " + m.getFailureReason();
                });
        register("material list", "study material list --course <course>", "F02: list a course's materials", cmd -> {
            List<CourseMaterial> list = facade.listMaterials(course(cmd).getId());
            if (list.isEmpty()) return "No materials yet.";
            return list.stream().map(m -> String.format("%-12s %-40s %-8s %s", m.getId(), m.getTitle(), m.getStatus(),
                    m.isIndexed() ? m.getChunkCount() + " chunks" : m.getFailureReason())).collect(Collectors.joining("\n"));
        });
        register("material delete", "study material delete <materialId>", "F02: delete a material and its index", cmd -> {
            facade.deleteMaterial(cmd.arg(0, "material id"));
            return "Deleted.";
        });
        register("material summarize", "study material summarize <materialId> [--length brief|standard|detailed]",
                "F03: AI summary with key points (map-reduce for long material)", cmd -> {
                    SummaryLength length = cmd.enumOption("length", SummaryLength.class, SummaryLength.STANDARD);
                    return formatSummary(facade.summarizeMaterial(cmd.arg(0, "material id"), new SummaryOptions(length)));
                });
        register("material extract-concepts", "study material extract-concepts <materialId>",
                "F04: AI extraction of key concepts and their relations", cmd -> {
                    ConceptGraph g = facade.extractConcepts(cmd.arg(0, "material id"));
                    StringBuilder sb = new StringBuilder(g.nodes.size() + " concept(s), " + g.edges.size() + " relation(s):\n");
                    g.nodes.forEach(k -> sb.append("- ").append(k.getName())
                            .append(k.getDefinition().isBlank() ? "" : ": " + k.getDefinition()).append('\n'));
                    return sb.toString().trim();
                });

        register("ask", "study ask --course <course> \"<question>\" | study ask --course <course> -i",
                "F05: ask the tutor; answers cite your material ([n]); -i keeps a conversation", cmd -> {
                    Course c = course(cmd);
                    String session = facade.newSessionId();
                    if (!cmd.has("interactive")) return formatAnswer(facade.askQuestion(session, c.getId(), cmd.text("question")));
                    print("Tutor for " + c.getCode() + ". Ask follow-up questions; empty line or 'exit' to stop.");
                    while (true) {
                        String q = prompt("you> ");
                        if (q == null || q.isBlank() || q.trim().equalsIgnoreCase("exit")) return "Bye.";
                        try {
                            print(formatAnswer(facade.askQuestion(session, c.getId(), q)));
                        } catch (AgentException | LLMException e) {
                            print("Error: " + e.getMessage());
                        }
                    }
                });
    }

    private String help() {
        Map<String, List<Command>> byGroup = new LinkedHashMap<>();
        commands.forEach((name, c) -> byGroup.computeIfAbsent(name.split(" ")[0], g -> new java.util.ArrayList<>()).add(c));
        StringBuilder sb = new StringBuilder("AI Study and Learning Agent - commands (add --help to any command, --trace to see the agent's plan)\n");
        byGroup.values().forEach(list -> list.forEach(c -> sb.append(String.format("  %-70s %s%n", c.usage(), c.description()))));
        sb.append("\nCourses can be given by id or by code (e.g. --course EECS3311). Run 'study' alone for an interactive shell.");
        return sb.toString();
    }

    static String describe(Course c) {
        String exam = c.getExamDate() == null ? "" : ", exam " + c.getExamDate() + " (" + c.daysUntilExam() + " days)";
        return c.getCode() + " - " + c.getTitle() + " [id " + c.getId() + exam + "]";
    }

    static String formatAnswer(Answer a) {
        StringBuilder sb = new StringBuilder(a.text);
        if (!a.citations.isEmpty()) sb.append("\n\nSources:\n  ").append(String.join("\n  ", a.citations));
        return sb.toString();
    }

    static String formatSummary(Summary s) {
        StringBuilder sb = new StringBuilder(s.getText()).append("\n\nKey points:\n");
        s.getKeyPoints().forEach(k -> sb.append("  - ").append(k).append('\n'));
        if (!s.getSourceRefs().isEmpty()) sb.append("Sources: ").append(String.join(", ", s.getSourceRefs()));
        return sb.toString().trim();
    }
}
