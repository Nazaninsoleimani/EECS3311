"""Draws the Stage 2 overview class diagrams (report figures) as SVG.

The full specification-level model is class-model-stage2.iuml (PlantUML). These three views keep only
the classes and relations needed to follow the features and design patterns, so they fit a report page.
Classes added in Stage 2 are blue. Run: python3 docs/stage2/diagrams/overview_svg.py
"""
import html
import math
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "img")
CHAR_W = 6.6      # average width of a 11px character
LINE_H = 14
PAD = 8
NEW_FILL, NEW_STROKE = "#DCEBFF", "#1F5FBF"
OLD_FILL, OLD_STROKE = "#FFFFFF", "#444444"
EXT_FILL = "#F2F2F2"


class Box:
    def __init__(self, key, x, y, name, stereo="", members=(), new=False, abstract=False, external=False, min_w=0):
        self.key, self.x, self.y, self.name, self.stereo = key, x, y, name, stereo
        self.members, self.new, self.abstract, self.external = list(members), new, abstract, external
        texts = [name, stereo] + self.members
        self.w = max(min_w, max(len(t) for t in texts) * CHAR_W + 2 * PAD)
        head = LINE_H * (2 if stereo else 1) + PAD
        self.h = head + (len(self.members) * LINE_H + PAD if self.members else 0) + 4
        self.head = head

    @property
    def cx(self): return self.x + self.w / 2

    @property
    def cy(self): return self.y + self.h / 2

    def border_point(self, tx, ty):
        """Where the line from the box centre towards (tx, ty) leaves the box."""
        dx, dy = tx - self.cx, ty - self.cy
        if dx == 0 and dy == 0:
            return self.cx, self.cy
        sx = (self.w / 2) / abs(dx) if dx else math.inf
        sy = (self.h / 2) / abs(dy) if dy else math.inf
        s = min(sx, sy)
        return self.cx + dx * s, self.cy + dy * s

    def svg(self):
        fill = NEW_FILL if self.new else EXT_FILL if self.external else OLD_FILL
        stroke = NEW_STROKE if self.new else OLD_STROKE
        out = [f'<rect x="{self.x:.1f}" y="{self.y:.1f}" width="{self.w:.1f}" height="{self.h:.1f}" rx="3" '
               f'fill="{fill}" stroke="{stroke}" stroke-width="1.2"/>']
        ty = self.y + PAD + 9
        if self.stereo:
            out.append(text(self.cx, ty, self.stereo, size=10, anchor="middle", style="italic"))
            ty += LINE_H
        out.append(text(self.cx, ty, self.name, size=11.5, anchor="middle", weight="bold",
                        style="italic" if self.abstract else "normal"))
        if self.members:
            ly = self.y + self.head
            out.append(f'<line x1="{self.x:.1f}" y1="{ly:.1f}" x2="{self.x + self.w:.1f}" y2="{ly:.1f}" stroke="{stroke}" stroke-width="0.8"/>')
            my = ly + PAD + 6
            for m in self.members:
                out.append(text(self.x + PAD, my, m, size=10.5))
                my += LINE_H
        return "\n".join(out)


def text(x, y, s, size=11, anchor="start", weight="normal", style="normal", fill="#111"):
    return (f'<text x="{x:.1f}" y="{y:.1f}" font-family="Helvetica, Arial, sans-serif" font-size="{size}" '
            f'text-anchor="{anchor}" font-weight="{weight}" font-style="{style}" fill="{fill}">{html.escape(s)}</text>')


# edge kinds: inh (generalization), real (realization), assoc, dep (dependency), comp (composition), agg (aggregation)
def edge_svg(a, b, kind, label="", la="", lb="", via=()):
    """Straight line from a to b, or a polyline through the via points (used to route around boxes)."""
    first = via[0] if via else (b.cx, b.cy)
    last = via[-1] if via else (a.cx, a.cy)
    x1, y1 = a.border_point(*first)
    x2, y2 = b.border_point(*last)
    pts = [(x1, y1)] + list(via) + [(x2, y2)]
    dashed = kind in ("real", "dep")
    marker_end = {"inh": "url(#tri)", "real": "url(#tri)", "assoc": "url(#open)", "dep": "url(#open)"}.get(kind, "")
    marker_start = {"comp": "url(#dfill)", "agg": "url(#dempty)"}.get(kind, "")
    attrs = 'stroke="#333" stroke-width="1.1" fill="none"' + (' stroke-dasharray="5,3"' if dashed else "")
    path = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
    out = [f'<polyline points="{path}" {attrs}'
           + (f' marker-end="{marker_end}"' if marker_end else "") + (f' marker-start="{marker_start}"' if marker_start else "") + "/>"]
    if label:
        i = (len(pts) - 1) // 2
        mx, my = (pts[i][0] + pts[i + 1][0]) / 2, (pts[i][1] + pts[i + 1][1]) / 2
        out.append(f'<rect x="{mx - len(label) * 2.9 - 2:.1f}" y="{my - 9:.1f}" width="{len(label) * 5.8 + 4:.1f}" height="12" fill="white" opacity="0.9"/>')
        out.append(text(mx, my, label, size=9.5, anchor="middle", style="italic", fill="#333"))
    for (px, py, qx, qy, t) in ((x1, y1, pts[1][0], pts[1][1], la), (x2, y2, pts[-2][0], pts[-2][1], lb)):
        if t:
            d = math.hypot(qx - px, qy - py) or 1
            out.append(text(px + (qx - px) / d * 16 + 4, py + (qy - py) / d * 16 - 3, t, size=9.5, fill="#333"))
    return "\n".join(out)


DEFS = """<defs>
<marker id="tri" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto-start-reverse">
  <path d="M1,1 L11,6 L1,11 z" fill="white" stroke="#333" stroke-width="1"/></marker>
<marker id="open" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="10" markerHeight="10" orient="auto-start-reverse">
  <path d="M1,1 L11,6 L1,11" fill="none" stroke="#333" stroke-width="1.2"/></marker>
<marker id="dfill" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="16" markerHeight="10" orient="auto-start-reverse">
  <path d="M1,5 L8,1 L15,5 L8,9 z" fill="#333" stroke="#333"/></marker>
<marker id="dempty" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="16" markerHeight="10" orient="auto-start-reverse">
  <path d="M1,5 L8,1 L15,5 L8,9 z" fill="white" stroke="#333"/></marker>
</defs>"""


def render(name, title, boxes, edges, layers):
    by = {b.key: b for b in boxes}
    via_x = [pt[0] for e in edges if isinstance(e[-1], list) for pt in e[-1]]
    width = max([b.x + b.w for b in boxes] + [x + 70 for x in via_x]) + 30
    height = max([b.y + b.h for b in boxes] + [y1 for (_, _, y1) in layers]) + 45
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width:.0f} {height:.0f}" width="{width:.0f}" height="{height:.0f}">',
             DEFS, f'<rect width="100%" height="100%" fill="white"/>', text(20, 26, title, size=15, weight="bold")]
    for (label, y0, y1) in layers:
        parts.append(f'<rect x="10" y="{y0}" width="{width - 20:.0f}" height="{y1 - y0}" fill="none" stroke="#999" stroke-dasharray="2,3" rx="4"/>')
        parts.append(text(18, y0 + 13, label, size=11, weight="bold", fill="#555"))
    for e in edges:
        via = e[-1] if isinstance(e[-1], list) else []
        args = e[2:-1] if via or isinstance(e[-1], list) else e[2:]
        parts.append(edge_svg(by[e[0]], by[e[1]], *args, via=via))
    for b in boxes:
        parts.append(b.svg())
    legend_y = height - 22
    parts.append(f'<rect x="20" y="{legend_y - 10}" width="14" height="12" fill="{NEW_FILL}" stroke="{NEW_STROKE}"/>')
    parts.append(text(40, legend_y, "added or changed in Stage 2", size=10.5))
    parts.append(f'<rect x="210" y="{legend_y - 10}" width="14" height="12" fill="{OLD_FILL}" stroke="{OLD_STROKE}"/>')
    parts.append(text(230, legend_y, "as designed in Stage 1", size=10.5))
    parts.append(f'<rect x="380" y="{legend_y - 10}" width="14" height="12" fill="{EXT_FILL}" stroke="{OLD_STROKE}"/>')
    parts.append(text(400, legend_y, "shown for context (other view)", size=10.5))
    parts.append("</svg>")
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name + ".svg"), "w") as f:
        f.write("\n".join(parts))
    # the same drawing turned 90 degrees, so a wide diagram fills a portrait report page
    body = "\n".join(parts[1:-1])
    with open(os.path.join(OUT, name + "-portrait.svg"), "w") as f:
        f.write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {height:.0f} {width:.0f}" width="{height:.0f}" height="{width:.0f}">\n'
                f'<g transform="translate({height:.0f},0) rotate(90)">\n{body}\n</g>\n</svg>')


def view_a():
    B = Box
    boxes = [
        B("gui", 30, 60, "StudyAgentGui", "", ["+ main(args)", "~ createViews(ctx)"], new=True),
        B("base", 230, 60, "BaseView", "{abstract}", ["# facade : LearningFacade", "+ render()", "+ showError(msg)", "# runAsync(work, onDone)"],
          abstract=True, new=True),
        B("views", 480, 60, "Concrete views (12)", "", ["DashboardView, CourseView, MaterialView,", "TutorChatView, StudyPlannerView, QuizView,",
                                                         "FlashcardView, LearningSessionView,", "ConceptMapView, ReminderView, HistoryView,",
                                                         "ProgressDashboardView <<ConcreteObserver>>"]),
        B("gctx", 30, 185, "GuiContext", "", ["+ selectCourse(c)", "+ fireDataChanged()", "+ requestQuizOn(concept)"], new=True),
        B("qpanel", 830, 75, "QuestionPanel", "", ["+ answer() : String", "+ showResult(r)"], new=True),
        B("notes", 830, 170, "StudyNotesHtml", "", ["+ summary(s, title) : String", "+ weakTopics(report) : String"], new=True),
        B("scmd", 30, 290, "StudyCommands", "", ["~ registerAll()", "- takeQuiz(q)", "- learningSession(c)"], new=True),
        B("cli", 210, 290, "CLIApplication", "", ["+ run(args) : int", "+ shell() : int", "- dispatch(cmd) : String"]),
        B("parser", 450, 290, "CommandParser", "", ["+ parse(args) : ParsedCommand", "+ tokenize(line) : String[]"]),
        B("pcmd", 700, 290, "ParsedCommand", "", ["+ group, action, args, options"]),
        B("appctx", 30, 420, "AppContext", "<<composition root>>", ["+ create(config, gateway)", "+ facade() : LearningFacade"], new=True),
        B("facade", 330, 415, "LearningFacade", "<<Facade>>", ["+ uploadMaterial(courseId, file) : MaterialUpload",
                                                               "+ storeMaterial(courseId, file) : CourseMaterial",
                                                               "+ askQuestion(session, course, q) : Answer",
                                                               "+ generateStudyPlan(req) : StudyPlan",
                                                               "+ submitQuiz(quizId, responses) : QuizAttempt",
                                                               "+ submitActivityResult(s, r) : ActivityOutcome",
                                                               "+ getProgress(student, course) : LearningProgress",
                                                               "... about 40 operations for F01-F16"], new=True),
        B("upload", 760, 410, "MaterialUpload", "<<record>>", ["material, concepts, conceptWarning"], new=True),
        B("outcome", 760, 485, "ActivityOutcome", "<<record>>", ["result, previousLevel, level, next"], new=True),
        B("agent", 790, 558, "LearningAgent", "(agent layer, view B)", [], external=True),
        B("course", 30, 680, "CourseService", "", ["+ createCourse(...)", "+ deleteCourse(id)"], new=True),
        B("material", 195, 680, "MaterialService", "", ["+ upload(courseId, file)", "+ retryPendingIndexing(ids)"], new=True),
        B("grading", 420, 680, "GradingService", "", ["+ grade(quiz, student, rs)", "- gradeWithRubric(q, r)"]),
        B("cmap", 625, 680, "ConceptMapService", "", ["+ save(courseId, graph)", "+ buildGraph(student, course)"]),
        B("qbank", 850, 680, "QuestionBank", "", ["+ pick(session, mastery) : Question"], new=True),
        B("tracker", 150, 820, "ProgressTracker", "<<Subject>>", ["+ attach(o)", "+ detach(o)", "+ recordResult(...)", "- notifyObservers(e)"]),
        B("pobs", 440, 835, "ProgressObserver", "<<interface>> <<Observer>>", ["+ onProgressChanged(e)"]),
        B("monitor", 760, 815, "StudyPlanMonitor", "<<ConcreteObserver>>", ["- needsRevision(plan, e)"]),
        B("reminders", 760, 915, "ReminderScheduler", "<<ConcreteObserver>>", ["+ schedule(r)", "+ dispatchDue(now)"]),
    ]
    edges = [
        ("gui", "base", "comp", "tabs", "1", "12"), ("views", "base", "inh"), ("base", "gctx", "assoc"),
        ("views", "qpanel", "dep", "uses"), ("views", "notes", "dep", "uses"), ("base", "facade", "assoc", "uses"),
        ("cli", "scmd", "comp"), ("cli", "parser", "assoc"), ("parser", "pcmd", "dep", "<<create>>"),
        ("cli", "facade", "assoc", "uses"), ("scmd", "facade", "dep", "", "", "", [(120, 400), (330, 440)]),
        ("appctx", "facade", "dep", "<<create>>"),
        ("facade", "upload", "dep", "<<create>>"), ("facade", "outcome", "dep", "<<create>>"), ("facade", "agent", "assoc"),
        ("facade", "course", "assoc"), ("facade", "material", "assoc"), ("facade", "grading", "assoc"),
        ("facade", "cmap", "assoc"), ("facade", "qbank", "assoc"),
        ("facade", "tracker", "assoc", "", "", "", [(410, 600), (410, 800)]),
        ("tracker", "pobs", "agg", "observers", "", "0..*"), ("monitor", "pobs", "real"), ("reminders", "pobs", "real"),
        ("monitor", "agent", "assoc", "revisePlan", "", "", [(1150, 845), (1150, 578)]),
    ]
    layers = [("Presentation layer (GUI and CLI)", 40, 365), ("Application layer", 385, 990)]
    render("class-view-a-presentation-application", "View A - Presentation and Application layers as implemented (Facade, Observer)",
           boxes, edges, layers)


def view_b():
    B = Box
    boxes = [
        B("planner", 30, 60, "Planner", "", ["+ createPlan(goal, ctx) : AgentPlan", "+ revise(plan, step, result)"]),
        B("registry", 30, 175, "ToolRegistry", "", ["+ register(t)", "+ getTool(name) : Tool"]),
        B("agent", 330, 60, "LearningAgent", "", ["+ answerQuestion(...) : Answer", "+ generateStudyPlan(req) : StudyPlan",
                                                  "+ revisePlan(plan, event, exam) : StudyPlan", "+ generateQuiz(req) : Quiz",
                                                  "+ nextAdaptiveActivity(s) : Question", "- executePlan(plan, ctx, binder)"], new=True),
        B("memory", 720, 60, "MemoryManager", "", ["+ loadLearnerContext(s, c)", "+ getSessionMemory(id)"]),
        B("ctxb", 720, 165, "ContextBuilder", "", ["+ build(task, ctx) : LLMRequest"]),
        B("validator", 980, 60, "ResponseValidator", "", ["+ validate(resp, schema)", "+ checkGrounding(answer, srcs)"]),
        B("tool", 30, 330, "Tool", "<<interface>>", ["+ execute(input) : ToolResult"]),
        B("tools", 30, 440, "Other tools (6)", "", ["RetrievalTool, SummarizationTool,", "ConceptExtractionTool,", "FlashcardGenerationTool,",
                                                     "ProgressAnalysisTool, ReminderTool"]),
        B("sched", 330, 330, "StudyScheduleTool", "<<Context>>", ["- strategy : StudyPlanningStrategy", "+ selectStrategy(req)"]),
        B("strat", 760, 330, "StudyPlanningStrategy", "<<interface>> <<Strategy>>", ["+ buildSchedule(req, priorities)"]),
        B("cram", 640, 450, "ExamCramStrategy", "<<ConcreteStrategy>>", []),
        B("spaced", 810, 450, "SpacedRepetitionStrategy", "<<ConcreteStrategy>>", []),
        B("balanced", 1010, 450, "BalancedReviewStrategy", "<<ConcreteStrategy>>", []),
        B("quizgen", 280, 470, "QuizGenerationTool", "<<Client>>", ["- factories : Map<QuestionType, QuestionFactory>"]),
        B("qfactory", 340, 590, "QuestionFactory", "{abstract} <<Creator>>", ["+ build(spec) : Question", "# createQuestion(spec, id)"], abstract=True),
        B("fmcq", 150, 715, "MultipleChoiceQuestionFactory", "", []),
        B("ftf", 490, 715, "TrueFalseQuestionFactory", "", []),
        B("fsa", 690, 715, "ShortAnswerQuestionFactory", "", []),
        B("question", 340, 800, "Question", "{abstract} <<Product>>", ["+ grade(response) : GradeResult", "+ correctAnswerText()"], abstract=True),
        B("mcq", 150, 930, "MultipleChoiceQuestion", "", []),
        B("tf", 375, 930, "TrueFalseQuestion", "", []),
        B("sa", 560, 930, "ShortAnswerQuestion", "", []),
        B("session", 900, 590, "AdaptiveLearningSession", "<<Context>>", ["- askedQuestionIds", "+ recordOutcome(result)", "+ setState(s)"], new=True),
        B("dstate", 900, 735, "DifficultyState", "<<interface>> <<State>>", ["+ onResult(session, result)", "+ preferredQuestionTypes()"]),
        B("found", 800, 870, "FoundationState", "", []),
        B("inter", 950, 870, "IntermediateState", "", []),
        B("adv", 1100, 870, "AdvancedState", "", []),
    ]
    edges = [
        ("agent", "planner", "assoc"), ("agent", "registry", "assoc"), ("agent", "memory", "assoc"), ("agent", "ctxb", "assoc"),
        ("agent", "validator", "assoc", "", "", "", [(700, 50), (1060, 50)]),
        ("registry", "tool", "agg", "tools", "", "0..*"), ("tools", "tool", "real"),
        ("sched", "tool", "real"), ("quizgen", "tool", "real"),
        ("sched", "strat", "assoc", "current"), ("cram", "strat", "real"), ("spaced", "strat", "real"), ("balanced", "strat", "real"),
        ("quizgen", "qfactory", "agg", "", "1", "1..*"), ("fmcq", "qfactory", "inh"), ("ftf", "qfactory", "inh"), ("fsa", "qfactory", "inh"),
        ("qfactory", "question", "dep", "<<create>>"), ("mcq", "question", "inh"), ("tf", "question", "inh"), ("sa", "question", "inh"),
        ("session", "dstate", "assoc", "state"), ("found", "dstate", "real"), ("inter", "dstate", "real"), ("adv", "dstate", "real"),
        ("agent", "session", "dep", "nextAdaptiveActivity", "", "", [(1220, 140), (1220, 630)]),
    ]
    layers = [("Agent layer", 40, 285), ("Agent tools and domain participants (Strategy, Factory Method, State)", 305, 990)]
    render("class-view-b-agent-tools-domain", "View B - Agent, tools and pattern participants as implemented (Strategy, Factory Method, State)",
           boxes, edges, layers)


def view_c():
    B = Box
    boxes = [
        B("factory", 30, 65, "LLMClientFactory", "", ["+ create(config) : LLMClient"], new=True),
        B("llm", 330, 65, "LLMClient", "<<interface>> <<Target>>", ["+ generate(req) : LLMResponse", "+ generateStructured(req, schema)",
                                                                     "+ getModelName()", "+ isAvailable()"]),
        B("meter", 640, 65, "UsageMeter", "", ["- inner : LLMClient", "+ getCalls(), getTokens()"], new=True),
        B("unavail", 640, 170, "UnavailableLLMClient", "", ["(no key: features use fallbacks)"], new=True),
        B("gem", 150, 220, "GeminiLLMAdapter", "<<Adapter>>", []),
        B("oai", 380, 220, "OpenAILLMAdapter", "<<Adapter>>", []),
        B("gsdk", 150, 320, "GeminiSdkClient", "<<Adaptee>>", ["+ generateContent(...)"]),
        B("osdk", 380, 320, "OpenAISdkClient", "<<Adaptee>>", ["+ createChatCompletion(...)"]),
        B("ngw", 960, 65, "NotificationGateway", "<<interface>>", ["+ send(title, body, to)"]),
        B("desk", 900, 170, "DesktopNotificationGateway", "", ["(macOS / tray)"]),
        B("cons", 1110, 170, "ConsoleNotificationGateway", "", ["(CLI)"], new=True),
        B("rnot", 900, 270, "ReminderNotifier", "", ["(GUI) + in-app pop-up", "+ signedIn(student, open)"], new=True),
        B("retr", 30, 450, "RetrievalService", "", ["+ index(material, chunks)", "+ retrieve(query, course, k)"]),
        B("emb", 330, 430, "EmbeddingService", "<<interface>>", ["+ embed(texts)"]),
        B("local", 280, 530, "LocalHashEmbeddingService", "", ["(default, offline)"], new=True),
        B("gemb", 500, 530, "GeminiEmbeddingService", "", [], new=True),
        B("vs", 30, 600, "VectorStore", "<<interface>>", ["+ upsert(...)", "+ search(vector, k, course)"]),
        B("svs", 330, 625, "SqliteVectorStore", "", [], new=True),
        B("parser", 760, 430, "DocumentParser", "<<interface>>", ["+ supports(type)", "+ parse(file)"]),
        B("parsers", 760, 540, "PdfParser, DocxParser, PlainTextParser", "", []),
        B("pdoc", 1040, 430, "ParsedDocument", "", ["+ pageTexts : List<String>"]),
        B("chunker", 1040, 540, "Chunker", "", ["+ chunk(doc, ...)"]),
        B("repos", 30, 760, "Repository interfaces (10)", "", ["StudentRepository, CourseRepository,", "MaterialRepository, ConceptRepository,",
                                                               "StudyPlanRepository, QuizRepository, ...", "ProgressRepository, ReminderRepository"]),
        B("srepos", 400, 780, "Sqlite*Repository (10)", "", ["store entities as JSON documents"], new=True),
        B("db", 720, 780, "SqliteDatabase", "", ["+ put(kind, id, parent, owner, value)", "+ get / byParent / byOwner"], new=True),
    ]
    edges = [
        ("factory", "llm", "dep", "<<create>>"), ("gem", "llm", "real"), ("oai", "llm", "real"), ("meter", "llm", "real"),
        ("unavail", "llm", "real"), ("gem", "gsdk", "assoc", "adapts"), ("oai", "osdk", "assoc", "adapts"),
        ("meter", "llm", "assoc", "wraps", "", "", [(700, 40), (420, 40)]),
        ("retr", "emb", "assoc"), ("retr", "vs", "assoc"), ("local", "emb", "real"), ("gemb", "emb", "real"), ("svs", "vs", "real"),
        ("parsers", "parser", "real"), ("parser", "pdoc", "dep", "<<create>>"), ("chunker", "pdoc", "dep"),
        ("desk", "ngw", "real"), ("cons", "ngw", "real"), ("rnot", "desk", "assoc", "delegates"), ("rnot", "ngw", "real", "", "", "", [(1080, 300), (1080, 140)]),
        ("srepos", "repos", "real"), ("srepos", "db", "assoc"), ("svs", "db", "assoc"),
    ]
    layers = [("AI / integration layer (Adapter)", 40, 705), ("Persistence layer", 725, 880)]
    render("class-view-c-integration-persistence", "View C - Integration and Persistence layers as implemented (Adapter)",
           boxes, edges, layers)


if __name__ == "__main__":
    view_a()
    view_b()
    view_c()
    print("wrote", sorted(os.listdir(OUT)))
