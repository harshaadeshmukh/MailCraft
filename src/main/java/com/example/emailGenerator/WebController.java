package com.example.emailGenerator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@AllArgsConstructor
public class WebController {

    private final EmailGeneratorService emailGeneratorService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("emailRequest", new EmailRequest());
        return "index";
    }

    @PostMapping("/generate")
    public String generate(
            @ModelAttribute EmailRequest emailRequest,
            Model model) throws Exception {

        // ── Server-side guard ──────────────────────────────────────
        if (emailRequest.getEmailContent() == null ||
                emailRequest.getEmailContent().isBlank()) {
            model.addAttribute("errorMessage", "Please paste an email before generating a reply.");
            model.addAttribute("emailRequest", emailRequest);
            return "index";
        }
        // ──────────────────────────────────────────────────────────

        String reply = emailGeneratorService.generateEmailReply(emailRequest);
        model.addAttribute("generatedReply", reply);
        model.addAttribute("emailRequest", emailRequest);
        return "index";
    }
}