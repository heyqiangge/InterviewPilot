package dev.interviewpilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.interviewpilot.Domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class Store {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public Store(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; }

    public void insertSession(Session s) {
        db.update("INSERT INTO sessions(id,candidate_id,target_role,current_question,current_topic,current_memory_ids) VALUES(?,?,?,?,?,?)",
                s.id(), s.candidateId(), s.targetRole(), s.question(), s.topic(), toJson(s.currentMemoryIds()));
    }
    public Optional<Session> session(String id) {
        return db.query("SELECT * FROM sessions WHERE id=?", (rs,n) -> new Session(rs.getString("id"),
                rs.getString("candidate_id"), rs.getString("target_role"), rs.getString("current_question"),
                rs.getString("current_topic"), fromJson(rs.getString("current_memory_ids")),
                rs.getTimestamp("created_at").toInstant()), id).stream().findFirst();
    }
    public void updateQuestion(String id, Question q, List<String> memoryIds) {
        db.update("UPDATE sessions SET current_question=?, current_topic=?, current_memory_ids=? WHERE id=?",
                q.text(), q.topic(), toJson(memoryIds), id);
    }
    public void insertTurn(Turn t) {
        db.update("INSERT INTO turns(id,session_id,question,topic,answer,score,feedback,retrieved_memory_ids) VALUES(?,?,?,?,?,?,?,?)",
                t.id(), t.sessionId(), t.question(), t.topic(), t.answer(), t.score(), t.feedback(),
                toJson(t.retrievedMemoryIds()));
    }
    public List<Turn> turns(String sessionId) {
        return db.query("SELECT * FROM turns WHERE session_id=? ORDER BY created_at,id", (rs,n) -> turn(rs), sessionId);
    }
    private Turn turn(ResultSet rs) throws SQLException {
        return new Turn(rs.getString("id"), rs.getString("session_id"), rs.getString("question"),
                rs.getString("topic"), rs.getString("answer"), rs.getInt("score"), rs.getString("feedback"),
                fromJson(rs.getString("retrieved_memory_ids")),
                rs.getTimestamp("created_at").toInstant());
    }
    public List<Memory> memories(String candidateId) {
        return db.query("SELECT * FROM memories WHERE candidate_id=? AND active=TRUE ORDER BY updated_at DESC",
                (rs,n) -> new Memory(rs.getString("id"), rs.getString("candidate_id"), rs.getString("topic"),
                        rs.getString("kind"), rs.getString("fact"), rs.getDouble("importance"),
                        rs.getDouble("confidence"), rs.getString("source_turn_id"),
                        rs.getTimestamp("updated_at").toInstant()), candidateId);
    }
    public void upsertMemory(Memory m) {
        db.update("MERGE INTO memories KEY(id) VALUES(?,?,?,?,?,?,?,?,TRUE,?)", m.id(), m.candidateId(),
                m.topic(), m.kind(), m.fact(), m.importance(), m.confidence(), m.sourceTurnId(),
                java.sql.Timestamp.from(Instant.now()));
    }
    public void deactivateMemory(String id) { db.update("UPDATE memories SET active=FALSE WHERE id=?", id); }
    private String toJson(List<String> values) {
        try { return json.writeValueAsString(values); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private List<String> fromJson(String value) {
        try { return json.readValue(value, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
