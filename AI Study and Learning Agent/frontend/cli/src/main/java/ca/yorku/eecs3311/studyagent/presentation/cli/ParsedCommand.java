package ca.yorku.eecs3311.studyagent.presentation.cli;

import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** A parsed "study <group> <action> [args] [--option value] [--flag]" command line. */
public class ParsedCommand {
    public final String group;
    public final String action;
    public final List<String> args;
    public final Map<String, String> options;
    public final Set<String> flags;

    public ParsedCommand(String group, String action, List<String> args, Map<String, String> options, Set<String> flags) {
        this.group = group;
        this.action = action;
        this.args = List.copyOf(args);
        this.options = Map.copyOf(options);
        this.flags = Set.copyOf(flags);
    }

    /** For single-word commands such as "ask": the word parsed as action is really the first argument. */
    public ParsedCommand withActionAsArgument() {
        if (action.isEmpty()) return this;
        List<String> all = new ArrayList<>();
        all.add(action);
        all.addAll(args);
        return new ParsedCommand(group, "", all, options, flags);
    }

    public boolean has(String flag) { return flags.contains(flag); }

    public String option(String name, String defaultValue) {
        return options.getOrDefault(name, defaultValue);
    }

    public String require(String name) {
        String v = options.get(name);
        if (v == null || v.isBlank()) throw new UsageException("Missing --" + name);
        return v;
    }

    public String arg(int i, String what) {
        if (i >= args.size()) throw new UsageException("Missing " + what);
        return args.get(i);
    }

    /** All positional arguments joined, e.g. an unquoted question. */
    public String text(String what) {
        if (args.isEmpty()) throw new UsageException("Missing " + what);
        return String.join(" ", args);
    }

    public int intOption(String name, int defaultValue) {
        String v = options.get(name);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("--" + name + " must be a whole number, got '" + v + "'");
        }
    }

    public LocalDate dateOption(String name) {
        String v = options.get(name);
        if (v == null || v.isBlank()) return null;
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("--" + name + " must be a date like 2026-12-15, got '" + v + "'");
        }
    }

    /** Case-insensitive enum value; "multiple-choice" matches MULTIPLE_CHOICE. */
    public static <E extends Enum<E>> E parseEnum(String value, Class<E> type, String what) {
        String key = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (E e : type.getEnumConstants()) if (e.name().equals(key)) return e;
        throw new ValidationException(what + " must be one of " + Arrays.stream(type.getEnumConstants())
                .map(e -> e.name().toLowerCase(Locale.ROOT).replace('_', '-')).toList() + ", got '" + value + "'");
    }

    public <E extends Enum<E>> E enumOption(String name, Class<E> type, E defaultValue) {
        String v = options.get(name);
        return v == null || v.isBlank() ? defaultValue : parseEnum(v, type, "--" + name);
    }

    public List<String> listOption(String name) {
        String v = options.get(name);
        if (v == null || v.isBlank()) return List.of();
        return Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Wrong command syntax (as opposed to invalid data); the CLI prints the command's usage. */
    public static class UsageException extends RuntimeException {
        public UsageException(String message) {
            super(message);
        }
    }
}
