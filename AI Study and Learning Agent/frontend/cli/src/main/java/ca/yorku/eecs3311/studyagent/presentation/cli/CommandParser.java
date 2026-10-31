package ca.yorku.eecs3311.studyagent.presentation.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns argument arrays (or a line typed in the interactive shell) into a ParsedCommand.
 * "--name value" is an option; "--name" followed by another option or nothing, a known boolean
 * flag, or "-i" is a flag. "--name=value" is also accepted.
 */
public class CommandParser {
    static final Set<String> BOOLEAN_FLAGS = Set.of("trace", "interactive", "help", "all");

    public ParsedCommand parse(String[] argv) {
        List<String> positional = new ArrayList<>();
        Map<String, String> options = new LinkedHashMap<>();
        Set<String> flags = new LinkedHashSet<>();
        for (int i = 0; i < argv.length; i++) {
            String a = argv[i];
            if (a.equals("-i")) {
                flags.add("interactive");
            } else if (a.equals("-h")) {
                flags.add("help");
            } else if (a.startsWith("--") && a.length() > 2) {
                String name = a.substring(2);
                int eq = name.indexOf('=');
                if (eq > 0) {
                    options.put(name.substring(0, eq), name.substring(eq + 1));
                } else if (!BOOLEAN_FLAGS.contains(name) && i + 1 < argv.length && !argv[i + 1].startsWith("--")) {
                    options.put(name, argv[++i]);
                } else {
                    flags.add(name);
                }
            } else {
                positional.add(a);
            }
        }
        String group = positional.isEmpty() ? "help" : positional.remove(0).toLowerCase();
        String action = positional.isEmpty() ? "" : positional.remove(0);
        return new ParsedCommand(group, action, positional, options, flags);
    }

    /** Splits a shell line into arguments, honouring "double" and 'single' quotes. */
    public static String[] tokenize(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        boolean inToken = false;
        for (char ch : line.toCharArray()) {
            if (quote != 0) {
                if (ch == quote) quote = 0;
                else cur.append(ch);
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
                inToken = true;
            } else if (Character.isWhitespace(ch)) {
                if (inToken) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    inToken = false;
                }
            } else {
                cur.append(ch);
                inToken = true;
            }
        }
        if (inToken) out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
