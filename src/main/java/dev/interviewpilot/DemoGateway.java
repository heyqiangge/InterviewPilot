package dev.interviewpilot;

import dev.interviewpilot.Domain.*;
import java.util.List;

/** Deterministic offline demonstration, never a substitute for model evaluation. */
public class DemoGateway implements Gateway {
    @Override public Question question(String role, String topic, String recent, String summary,
                                        List<Memory> memories, List<Knowledge> knowledge) {
        String opener = memories.stream().filter(m -> m.kind().equals("gap") && m.topic().equals(topic))
                .findFirst().map(m -> "上次你在" + topic + "上的薄弱点是“" + m.fact() + "”。")
                .orElse("请结合你的项目或实习经历。");
        return new Question(topic, opener + "请解释" + topic + "的核心原理、一个边界条件，以及线上如何验证？");
    }
    @Override public Assessment assess(String question, String answer, List<Knowledge> knowledge) {
        int length = answer.trim().length();
        int score = length < 20 ? 1 : length < 80 ? 2 : length < 180 ? 3 : 4;
        String gap = length < 80 ? "回答缺少原理、边界条件或验证方法" : "需要结合明确的实验与证据";
        return new Assessment(score, "[演示模式：非真实评分] " + gap, gap);
    }
    @Override public List<MemoryDraft> extract(String question, String answer, Assessment assessment) {
        if (answer.trim().length() >= 80) return List.of();
        return List.of(new MemoryDraft(topicFromQuestion(question), "gap", assessment.gap(), 0.8, 0.6,
                answer.length() > 40 ? answer.substring(0,40) : answer));
    }
    private String topicFromQuestion(String q) {
        for (String t : KnowledgeBase.TOPICS) if (q.contains(t)) return t;
        return "综合";
    }
    @Override public String mode() { return "demo"; }
}
