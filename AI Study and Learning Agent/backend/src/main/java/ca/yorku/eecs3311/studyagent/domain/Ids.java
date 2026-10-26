package ca.yorku.eecs3311.studyagent.domain;

import java.util.UUID;

/** Short, human-typeable identifiers (the CLI shows and accepts them). */
public final class Ids {
    private Ids() {}

    public static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
