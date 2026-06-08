package com.example.emailGenerator;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", exposedHeaders = {"X-RateLimit-Limit", "X-RateLimit-Remaining"})
@RestController
@RequestMapping("/api/email")
@AllArgsConstructor
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(
            @RequestBody EmailRequest emailRequest) {
        try {
            String response = emailGeneratorService.generateEmailReply(emailRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(503)
                    .body("⏳ All keys are busy. Please try again in a minute.");
        }
    }
}