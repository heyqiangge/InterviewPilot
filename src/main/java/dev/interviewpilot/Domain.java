package dev.interviewpilot;

import java.time.Instant;
import java.util.List;

public final class Domain {
    private Domain() {}
    public record Session(String id, String candidateId, String targetRole, String question, String topic,
                          List<String> currentMemoryIds, Instant createdAt) {}
    public record Turn(String id, String sessionId, String question, String topic, String answer,
                       int score, String feedback, List<String> retrievedMemoryIds, Instant createdAt) {}
    public record Memory(String id, String candidateId, String topic, String kind, String fact,
                         double importance, double confidence, String sourceTurnId, Instant updatedAt) {}
    public record MemoryDraft(String topic, String kind, String fact, double importance,
                              double confidence, String sourceQuote) {}
    public record Assessment(int score, String feedback, String gap) {}
    public record Question(String topic, String text) {}
    public record Knowledge(String topic, String title, String text) {}
    public record AnswerResult(Turn turn, Question nextQuestion, List<Memory> contextMemories,
                               List<Knowledge> knowledge, String mode) {}
}
