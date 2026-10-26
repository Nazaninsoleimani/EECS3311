package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Single local SQLite file. Entities are stored as JSON documents keyed by (kind, id) with
 * a parent and owner column for the lookups the repositories need; vectors have their own table.
 */
public class SqliteDatabase implements AutoCloseable {
    private final Connection connection;

    public SqliteDatabase(String jdbcUrl) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
            createSchema();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot open database " + jdbcUrl, e);
        }
    }

    public static SqliteDatabase open(Path file) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
        } catch (Exception e) {
            throw new PersistenceException("Cannot create data directory for " + file, e);
        }
        return new SqliteDatabase("jdbc:sqlite:" + file.toAbsolutePath());
    }

    public static SqliteDatabase inMemory() {
        return new SqliteDatabase("jdbc:sqlite::memory:");
    }

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS documents (
                    kind TEXT NOT NULL, id TEXT NOT NULL, parent TEXT, owner TEXT,
                    json TEXT NOT NULL, created TEXT NOT NULL, PRIMARY KEY (kind, id))""");
            st.execute("CREATE INDEX IF NOT EXISTS idx_doc_parent ON documents(kind, parent)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_doc_owner ON documents(kind, owner)");
            st.execute("""
                CREATE TABLE IF NOT EXISTS vectors (
                    chunk_id TEXT PRIMARY KEY, material_id TEXT NOT NULL, course_id TEXT NOT NULL,
                    vector BLOB NOT NULL)""");
            st.execute("CREATE INDEX IF NOT EXISTS idx_vec_course ON vectors(course_id)");
        }
    }

    public Connection connection() { return connection; }

    public synchronized void put(String kind, String id, String parent, String owner, Object value) {
        String sql = """
            INSERT INTO documents(kind, id, parent, owner, json, created) VALUES (?,?,?,?,?,?)
            ON CONFLICT(kind, id) DO UPDATE SET parent=excluded.parent, owner=excluded.owner, json=excluded.json""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kind);
            ps.setString(2, id);
            ps.setString(3, parent);
            ps.setString(4, owner);
            ps.setString(5, JsonCodec.GSON.toJson(value));
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save " + kind + " " + id, e);
        }
    }

    public synchronized <T> Optional<T> get(String kind, String id, Class<T> type) {
        List<T> found = query("SELECT json FROM documents WHERE kind=? AND id=?", type, kind, id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    public synchronized <T> List<T> byParent(String kind, String parent, Class<T> type) {
        return query("SELECT json FROM documents WHERE kind=? AND parent=? ORDER BY created, rowid", type, kind, parent);
    }

    public synchronized <T> List<T> byOwner(String kind, String owner, Class<T> type) {
        return query("SELECT json FROM documents WHERE kind=? AND owner=? ORDER BY created, rowid", type, kind, owner);
    }

    public synchronized <T> List<T> all(String kind, Class<T> type) {
        return query("SELECT json FROM documents WHERE kind=? ORDER BY created, rowid", type, kind);
    }

    public synchronized void delete(String kind, String id) {
        update("DELETE FROM documents WHERE kind=? AND id=?", kind, id);
    }

    public synchronized void deleteByParent(String kind, String parent) {
        update("DELETE FROM documents WHERE kind=? AND parent=?", kind, parent);
    }

    public synchronized void deleteByOwner(String kind, String owner) {
        update("DELETE FROM documents WHERE kind=? AND owner=?", kind, owner);
    }

    public synchronized void update(String sql, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Update failed: " + sql, e);
        }
    }

    private <T> List<T> query(String sql, Class<T> type, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            List<T> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(JsonCodec.GSON.fromJson(rs.getString(1), type));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + sql, e);
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // closing on shutdown; nothing useful to do
        }
    }
}
