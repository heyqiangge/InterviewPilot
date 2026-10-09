package dev.interviewpilot;

import dev.interviewpilot.Domain.Knowledge;
import dev.interviewpilot.Domain.Memory;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Keyword + optional embedding ranking, followed by budgeted memory selection. */
public class Retriever {
    private final Gateway gateway;
    private final Map<String,float[]> documentVectors = new HashMap<>();
    public Retriever(Gateway gateway) { this.gateway = gateway; }

    public List<Knowledge> knowledge(String query, String topic, int limit) {
        float[] q = gateway.mode().equals("openai") ? gateway.embed(query) : null;
        return KnowledgeBase.DOCUMENTS.stream()
                .map(d -> Map.entry(d, 0.55 * lexical(query, d.title()+" "+d.text())
                        + 0.20 * (d.topic().equals(topic) ? 1 : 0)
                        + 0.25 * similarity(q, vector(d))))
                .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit).map(Map.Entry::getKey).toList();
    }
    public List<Memory> memories(String query, String topic, List<Memory> source, int maxChars, int limit) {
        float[] q = gateway.mode().equals("openai") ? gateway.embed(query) : null;
        List<Map.Entry<Memory,Double>> ranked = source.stream().map(m -> {
            double age = Math.max(0, Duration.between(m.updatedAt(), Instant.now()).toDays());
            double score = 0.40 * lexical(query, m.topic()+" "+m.fact())
                    + 0.20 * (m.topic().equals(topic) ? 1 : 0)
                    + 0.20 * similarity(q, gateway.mode().equals("openai") ? gateway.embed(m.fact()) : null)
                    + 0.15 * m.importance() + 0.05 * Math.exp(-age/90.0);
            return Map.entry(m,score);
        }).sorted((a,b) -> Double.compare(b.getValue(), a.getValue())).toList();
        List<Memory> picked = new ArrayList<>();
        int chars = 0;
        for (var entry : ranked) {
            Memory m = entry.getKey();
            if (picked.size() >= limit) break;
            if (entry.getValue() < 0.12 || chars + m.fact().length() > maxChars) continue;
            if (picked.stream().anyMatch(p -> p.topic().equals(m.topic()) && lexical(p.fact(),m.fact()) > 0.8)) continue;
            picked.add(m); chars += m.fact().length();
        }
        return picked;
    }
    private float[] vector(Knowledge d) {
        if (!gateway.mode().equals("openai")) return null;
        return documentVectors.computeIfAbsent(d.title(), ignored -> gateway.embed(d.title()+" "+d.text()));
    }
    static double lexical(String a, String b) {
        Set<String> left = tokens(a), right = tokens(b);
        if (left.isEmpty() || right.isEmpty()) return 0;
        long overlap = left.stream().filter(right::contains).count();
        return overlap / Math.sqrt((double)left.size() * right.size());
    }
    static Set<String> tokens(String text) {
        String s = text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsHan}a-z0-9]", "");
        Set<String> out = new HashSet<>();
        for (String term : s.split("(?<=\\p{IsHan})|(?=\\p{IsHan})")) {
            if (term.length() > 1) out.add(term);
        }
        for (int i=0;i+2<=s.length();i++) out.add(s.substring(i,i+2));
        return out;
    }
    static double similarity(float[] a, float[] b) {
        if (a==null || b==null || a.length!=b.length) return 0;
        double dot=0,x=0,y=0;
        for(int i=0;i<a.length;i++){dot+=a[i]*b[i];x+=a[i]*a[i];y+=b[i]*b[i];}
        return x==0 || y==0 ? 0 : Math.max(0,dot/Math.sqrt(x*y));
    }
}
