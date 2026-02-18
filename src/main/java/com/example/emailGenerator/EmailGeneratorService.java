package com.example.emailGenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;
    private final String apiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder,
                                 @Value("${gemini.api.url}") String baseURL,
                                 @Value("${gemini.api.key}") String geminiAPI) {
        this.apiKey = geminiAPI;
        this.webClient = webClientBuilder.baseUrl(baseURL).build();
    }

    public String generateEmailReply(EmailRequest emailRequest) throws Exception {

        // Build Prompt
        String prompt = buildPrompt(emailRequest);

        // ✅ Use ObjectMapper to safely build JSON
        // This automatically escapes quotes, newlines, backslashes etc.
        // so special characters in emails never break the request
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode part    = mapper.createObjectNode().put("text", prompt);
        ObjectNode content = mapper.createObjectNode()
                .set("parts", mapper.createArrayNode().add(part));
        ObjectNode body    = mapper.createObjectNode()
                .set("contents", mapper.createArrayNode().add(content));

        String requestBody = mapper.writeValueAsString(body);

        // Send request
        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-2.5-flash:generateContent")
                        .build())
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Extract response
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        return root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email:");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" Tone.");
        }
        prompt.append("Original Email :\n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}