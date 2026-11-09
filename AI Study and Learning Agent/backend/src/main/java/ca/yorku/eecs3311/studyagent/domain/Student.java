package ca.yorku.eecs3311.studyagent.domain;

public class Student {
    private final String id;
    private String name;
    private String email;
    private StudentProfile profile;
    /** PBKDF2 hash and salt (Base64); null for the local profile used by the CLI, which has no password. */
    private String passwordHash;
    private String passwordSalt;

    public Student(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profile = new StudentProfile();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public StudentProfile getProfile() { return profile; }
    public void setProfile(StudentProfile profile) { this.profile = profile; }
    public String getPasswordHash() { return passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }
    public void setCredentials(String hash, String salt) {
        this.passwordHash = hash;
        this.passwordSalt = salt;
    }
    public boolean hasAccount() { return passwordHash != null; }
}
