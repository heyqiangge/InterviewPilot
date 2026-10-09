package dev.interviewpilot;

import dev.interviewpilot.Domain.*;
import java.util.List;

public interface Gateway {
    Question question(String role, String topic, String recentTurns, String historySummary,
                      List<Memory> memories, List<Knowledge> knowledge);
    Assessment assess(String question, String answer, List<Knowledge> knowledge);
    List<MemoryDraft> extract(String question, String answer, Assessment assessment);
    default float[] embed(String text) { return null; }
    String mode();
}
