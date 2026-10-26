package ca.yorku.eecs3311.studyagent.domain;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;

/** Study preferences the agent uses when planning and adapting. */
public class StudentProfile {
    private int preferredSessionMinutes = 45;
    private Map<DayOfWeek, Integer> weeklyAvailability = new EnumMap<>(DayOfWeek.class);
    private DifficultyLevel preferredDifficulty = DifficultyLevel.FOUNDATION;
    private boolean remindersEnabled = true;

    public int getPreferredSessionMinutes() { return preferredSessionMinutes; }
    public void setPreferredSessionMinutes(int minutes) { this.preferredSessionMinutes = minutes; }
    public Map<DayOfWeek, Integer> getWeeklyAvailability() { return weeklyAvailability; }
    public void setWeeklyAvailability(Map<DayOfWeek, Integer> availability) { this.weeklyAvailability = availability; }
    public DifficultyLevel getPreferredDifficulty() { return preferredDifficulty; }
    public void setPreferredDifficulty(DifficultyLevel level) { this.preferredDifficulty = level; }
    public boolean isRemindersEnabled() { return remindersEnabled; }
    public void setRemindersEnabled(boolean enabled) { this.remindersEnabled = enabled; }

    /** Minutes available on a given day; falls back to the preferred session length. */
    public int minutesOn(DayOfWeek day) {
        return weeklyAvailability.getOrDefault(day, preferredSessionMinutes);
    }
}
