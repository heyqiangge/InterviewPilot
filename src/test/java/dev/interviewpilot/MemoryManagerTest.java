package dev.interviewpilot;

import dev.interviewpilot.Domain.MemoryDraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryManagerTest {
    @Test void rejectsMemoryWithoutVerbatimEvidence() {
        assertFalse(MemoryManager.valid(new MemoryDraft("MySQL索引","strength","我掌握索引",0.8,0.9,"我掌握索引"), "我不清楚索引"));
    }
    @Test void acceptsBoundedMemoryWithEvidence() {
        assertTrue(MemoryManager.valid(new MemoryDraft("MySQL索引","gap","不清楚回表",0.8,0.9,"不清楚回表"), "我不清楚回表"));
    }
    @Test void lexicalSimilarityFindsSharedChineseConcepts() {
        assertTrue(Retriever.lexical("MySQL索引回表", "什么情况下索引会发生回表") > 0);
    }
}
