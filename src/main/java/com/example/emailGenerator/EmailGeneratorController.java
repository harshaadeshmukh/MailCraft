package com.example.emailGenerator;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/email")
@AllArgsConstructor
public class EmailGeneratorController {
    private  final EmailGeneratorService emailGeneratorService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("emailRequest", new EmailRequest());
        return "index";
    }

    @PostMapping("/generate")
    public String generate(@ModelAttribute EmailRequest emailRequest, Model model) throws Exception {
        String reply = emailGeneratorService.generateEmailReply(emailRequest);
        model.addAttribute("generatedReply", reply);
        model.addAttribute("emailRequest", emailRequest);
        return "index";
    }

}
