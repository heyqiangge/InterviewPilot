package dev.interviewpilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    @Bean Gateway gateway(@Value("${interviewpilot.mode}") String mode,
                          @Value("${interviewpilot.chat-model}") String chatModel,
                          @Value("${interviewpilot.embedding-model}") String embeddingModel,
                          ObjectMapper json) {
        if (mode.equals("demo")) return new DemoGateway();
        if (!mode.equals("openai")) throw new IllegalArgumentException("INTERVIEWPILOT_MODE must be demo or openai");
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("OPENAI_API_KEY is required in openai mode");
        return new OpenAiGateway(key, chatModel, embeddingModel, json);
    }
}
