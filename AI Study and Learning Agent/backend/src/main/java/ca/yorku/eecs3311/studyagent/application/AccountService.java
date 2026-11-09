package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.Student;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.persistence.StudentRepository;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Student accounts for the GUI: create an account and sign in. Passwords are never stored; only a
 * salted PBKDF2 hash. Each account has its own courses, progress and history.
 */
public class AccountService {
    static final int MIN_PASSWORD_LENGTH = 8;
    private static final int ITERATIONS = 120_000;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String WRONG_LOGIN = "Email or password is incorrect";

    private final StudentRepository students;
    private final SecureRandom random = new SecureRandom();

    public AccountService(StudentRepository students) {
        this.students = students;
    }

    public Student register(String name, String email, char[] password, char[] confirmation) {
        if (name == null || name.isBlank()) throw new ValidationException("Enter your name");
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(mail).matches()) throw new ValidationException("Enter a valid email address");
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (confirmation != null && !Arrays.equals(password, confirmation)) throw new ValidationException("Passwords do not match");
        if (students.findByEmail(mail).isPresent()) throw new ValidationException("An account with this email already exists");
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        Student s = new Student(Ids.newId("s"), name.trim(), mail);
        s.setCredentials(hash(password, salt), Base64.getEncoder().encodeToString(salt));
        students.save(s);
        return s;
    }

    /** The same message for an unknown email and a wrong password, so accounts cannot be probed. */
    public Student signIn(String email, char[] password) {
        Student s = students.findByEmail(email).filter(Student::hasAccount)
                .orElseThrow(() -> new ValidationException(WRONG_LOGIN));
        byte[] salt = Base64.getDecoder().decode(s.getPasswordSalt());
        boolean ok = MessageDigest.isEqual(hash(password == null ? new char[0] : password, salt).getBytes(),
                s.getPasswordHash().getBytes());
        if (!ok) throw new ValidationException(WRONG_LOGIN);
        return s;
    }

    static String hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        }
    }
}
