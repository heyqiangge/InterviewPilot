package dev.interviewpilot;

import dev.interviewpilot.Domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InterviewEngine {
    private final Store store;
    private final Gateway gateway;
    private final Retriever retriever;
    private final MemoryManager memoryManager;
    public InterviewEngine(Store store, Gateway gateway) {
        this.store=store; this.gateway=gateway;
        this.retriever=new Retriever(gateway); this.memoryManager=new MemoryManager(store);
    }
    @Transactional
    public synchronized Session start(String candidateId, String targetRole) {
        require(candidateId, 100, "candidateId"); require(targetRole, 200, "targetRole");
        String topic = KnowledgeBase.TOPICS.get(0);
        List<Memory> memories = retriever.memories(targetRole+" "+topic, topic,
                store.memories(candidateId), 1200, 5);
        List<Knowledge> knowledge = retriever.knowledge(topic, topic, 2);
        Question q = gateway.question(targetRole, topic, "", "", memories, knowledge);
        Session s = new Session(UUID.randomUUID().toString(), candidateId, targetRole, q.text(), q.topic(),
                memories.stream().map(Memory::id).toList(), Instant.now());
        store.insertSession(s);
        return s;
    }
    @Transactional
    public synchronized AnswerResult answer(String sessionId, String answer) {
        require(answer, 5000, "answer");
        Session s = store.session(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
        List<Knowledge> currentKnowledge = retriever.knowledge(s.question(), s.topic(), 3);
        Assessment assessment = gateway.assess(s.question(), answer, currentKnowledge);
        String turnId = UUID.randomUUID().toString();
        Turn turn = new Turn(turnId, s.id(), s.question(), s.topic(), answer, assessment.score(),
                assessment.feedback(), s.currentMemoryIds(), Instant.now());
        store.insertTurn(turn);
        memoryManager.merge(s.candidateId(), turnId, answer, gateway.extract(s.question(), answer, assessment));
        List<Turn> history = store.turns(s.id());
        String nextTopic = assessment.score() < 3 ? s.topic() : KnowledgeBase.TOPICS.get(
                (KnowledgeBase.TOPICS.indexOf(s.topic())+1) % KnowledgeBase.TOPICS.size());
        String recent = history.stream().skip(Math.max(0,history.size()-4)).map(t ->
                "主题="+t.topic()+" 问="+t.question()+" 答="+t.answer()+" 分="+t.score())
                .reduce("", (a,b) -> a+"\n"+b);
        String summary = history.stream().limit(Math.max(0,history.size()-4)).map(t ->
                t.topic()+"(评分"+t.score()+")").reduce("", (a,b) -> a+";"+b);
        List<Memory> memories = retriever.memories(nextTopic+" "+assessment.gap(), nextTopic,
                store.memories(s.candidateId()), 1200, 5);
        List<Knowledge> knowledge = retriever.knowledge(nextTopic+" "+assessment.gap(), nextTopic, 3);
        Question next = gateway.question(s.targetRole(),nextTopic,recent,summary,memories,knowledge);
        store.updateQuestion(s.id(),next,memories.stream().map(Memory::id).toList());
        return new AnswerResult(turn,next,memories,knowledge,gateway.mode());
    }
    public Session session(String id) {
        return store.session(id).orElseThrow(() -> new IllegalArgumentException("session not found"));
    }
    public List<Turn> turns(String id) { session(id); return store.turns(id); }
    public List<Memory> memories(String candidateId) { require(candidateId,100,"candidateId"); return store.memories(candidateId); }
    public String mode() { return gateway.mode(); }
    private static void require(String value, int max, String name) {
        if(value==null || value.isBlank() || value.length()>max) throw new IllegalArgumentException(name+" must be 1.."+max+" chars");
    }
}
