package dev.interviewpilot;

import dev.interviewpilot.Domain.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MemoryManager {
    private final Store store;
    public MemoryManager(Store store) { this.store = store; }

    public void merge(String candidateId, String turnId, String answer, List<MemoryDraft> drafts) {
        for (MemoryDraft d : drafts.stream().limit(3).toList()) {
            if (!valid(d, answer)) continue;
            List<Memory> existing = store.memories(candidateId);
            Memory same = existing.stream().filter(m -> m.topic().equalsIgnoreCase(d.topic())
                    && m.kind().equals(d.kind()) && Retriever.lexical(m.fact(), d.fact()) >= 0.55)
                    .findFirst().orElse(null);
            if (same != null) {
                store.upsertMemory(new Memory(same.id(), candidateId, d.topic(), d.kind(), d.fact(),
                        Math.max(same.importance(), d.importance()), Math.max(same.confidence(), d.confidence()),
                        turnId, Instant.now()));
                continue;
            }
            // A strong, recent observation can supersede a conflicting claim about the same concept.
            if (d.confidence() >= 0.8) existing.stream().filter(m -> m.topic().equalsIgnoreCase(d.topic())
                    && !m.kind().equals(d.kind()) && Retriever.lexical(m.fact(),d.fact()) >= 0.45)
                    .forEach(m -> store.deactivateMemory(m.id()));
            store.upsertMemory(new Memory(UUID.randomUUID().toString(), candidateId, d.topic(), d.kind(),
                    d.fact(), d.importance(), d.confidence(), turnId, Instant.now()));
        }
    }
    static boolean valid(MemoryDraft d, String answer) {
        return d != null && d.topic()!=null && !d.topic().isBlank() && d.topic().length() <= 100
                && ("strength".equals(d.kind()) || "gap".equals(d.kind()))
                && d.fact()!=null && !d.fact().isBlank() && d.fact().length() <= 500
                && d.sourceQuote()!=null && !d.sourceQuote().isBlank() && answer.contains(d.sourceQuote())
                && d.importance() >= 0 && d.importance() <= 1 && d.confidence() >= 0 && d.confidence() <= 1;
    }
}
