package ca.yorku.eecs3311.studyagent.integration.retrieval;

import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.persistence.MaterialRepository;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.PersistenceException;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Vectors stored as float blobs in the local database; exact cosine search per course. */
public class SqliteVectorStore implements VectorStore {
    private final SqliteDatabase db;
    private final MaterialRepository materials;

    public SqliteVectorStore(SqliteDatabase db, MaterialRepository materials) {
        this.db = db;
        this.materials = materials;
        try (Statement st = db.connection().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS chunk_vectors (chunk_id TEXT PRIMARY KEY, material_id TEXT NOT NULL, "
                    + "course_id TEXT NOT NULL, model TEXT NOT NULL, vector BLOB NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_chunk_vec_course ON chunk_vectors(course_id, model)");
        } catch (SQLException e) {
            throw new PersistenceException("Cannot create vector table", e);
        }
    }

    @Override
    public synchronized void upsert(List<DocumentChunk> chunks, List<float[]> vectors, String model) {
        String sql = "INSERT OR REPLACE INTO chunk_vectors(chunk_id, material_id, course_id, model, vector) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk c = chunks.get(i);
                ps.setString(1, c.getId());
                ps.setString(2, c.getMaterialId());
                ps.setString(3, c.getCourseId());
                ps.setString(4, model);
                ps.setBytes(5, toBytes(vectors.get(i)));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot store vectors", e);
        }
    }

    @Override
    public synchronized List<RetrievedChunk> search(float[] query, int k, String courseId, String model) {
        PriorityQueue<Map.Entry<String, Double>> best = new PriorityQueue<>(Map.Entry.comparingByValue());
        String sql = "SELECT chunk_id, vector FROM chunk_vectors WHERE course_id=? AND model=?";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, courseId);
            ps.setString(2, model);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double score = cosine(query, fromBytes(rs.getBytes(2)));
                    best.add(Map.entry(rs.getString(1), score));
                    if (best.size() > k) best.poll();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Vector search failed", e);
        }
        Map<String, DocumentChunk> chunksById = new HashMap<>();
        for (DocumentChunk c : materials.findChunksByCourse(courseId)) chunksById.put(c.getId(), c);
        List<RetrievedChunk> result = new ArrayList<>();
        for (Map.Entry<String, Double> e : best) {
            DocumentChunk c = chunksById.get(e.getKey());
            if (c != null) result.add(new RetrievedChunk(c, e.getValue()));
        }
        result.sort(Comparator.comparingDouble((RetrievedChunk r) -> r.score).reversed());
        return result;
    }

    @Override
    public void deleteByMaterial(String materialId) {
        db.update("DELETE FROM chunk_vectors WHERE material_id=?", materialId);
    }

    @Override
    public void deleteByCourse(String courseId) {
        db.update("DELETE FROM chunk_vectors WHERE course_id=?", courseId);
    }

    static double cosine(float[] a, float[] b) {
        if (a.length != b.length) return -1;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / Math.sqrt(na * nb);
    }

    private static byte[] toBytes(float[] v) {
        ByteBuffer buf = ByteBuffer.allocate(v.length * 4);
        for (float f : v) buf.putFloat(f);
        return buf.array();
    }

    private static float[] fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        float[] v = new float[bytes.length / 4];
        for (int i = 0; i < v.length; i++) v[i] = buf.getFloat();
        return v;
    }
}
