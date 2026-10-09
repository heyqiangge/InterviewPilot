package dev.interviewpilot;

import dev.interviewpilot.Domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InterviewController {
    private final InterviewEngine engine;
    public InterviewController(InterviewEngine engine) { this.engine=engine; }
    public record StartRequest(String candidateId, String targetRole) {}
    public record AnswerRequest(String answer) {}
    @GetMapping("/health") public Map<String,String> health() { return Map.of("status","ok","mode",engine.mode()); }
    @PostMapping("/sessions") public Session start(@RequestBody StartRequest request) {
        return engine.start(request.candidateId(),request.targetRole());
    }
    @GetMapping("/sessions/{id}") public Session session(@PathVariable String id) { return engine.session(id); }
    @GetMapping("/sessions/{id}/turns") public List<Turn> turns(@PathVariable String id) { return engine.turns(id); }
    @PostMapping("/sessions/{id}/answers") public AnswerResult answer(@PathVariable String id,
                                                                         @RequestBody AnswerRequest request) {
        return engine.answer(id,request.answer());
    }
    @GetMapping("/candidates/{id}/memories") public List<Memory> memories(@PathVariable String id) {
        return engine.memories(id);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> badRequest(IllegalArgumentException e) { return Map.of("error",e.getMessage()); }
}
