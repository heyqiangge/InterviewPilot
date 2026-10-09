package dev.interviewpilot;

import dev.interviewpilot.Domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:interviewtest;DB_CLOSE_DELAY=-1", "interviewpilot.mode=demo"})
@AutoConfigureMockMvc
class InterviewFlowTest {
    @Autowired InterviewEngine engine;
    @Autowired MockMvc http;

    @Test void crossSessionMemoryIsRecalledWithoutClaimingRealScore() {
        String candidate = "test-"+UUID.randomUUID();
        Session first = engine.start(candidate, "后端开发工程师");
        assertEquals("MySQL索引", first.topic());
        AnswerResult answer = engine.answer(first.id(), "我不清楚索引回表。需要进一步学习。 ");
        assertEquals("demo", answer.mode());
        assertTrue(answer.turn().feedback().contains("演示模式"));
        assertEquals(1, engine.turns(first.id()).size());
        assertFalse(engine.memories(candidate).isEmpty());
        Session second = engine.start(candidate, "后端开发工程师");
        assertFalse(second.currentMemoryIds().isEmpty());
        assertTrue(second.question().contains("薄弱点"));
    }
    @Test void rejectsBlankAnswers() {
        Session s = engine.start("candidate-"+UUID.randomUUID(),"后端开发工程师");
        assertThrows(IllegalArgumentException.class, () -> engine.answer(s.id()," "));
    }
    @Test void httpApiReturnsQuestionAndRejectsInvalidInput() throws Exception {
        http.perform(get("/api/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("demo"));
        http.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":\"test-http\",\"targetRole\":\"后端开发工程师\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.question").isNotEmpty());
        http.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":\"\",\"targetRole\":\"后端\"}"))
                .andExpect(status().isBadRequest());
    }
}
