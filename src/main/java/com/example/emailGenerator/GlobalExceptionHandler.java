package com.example.emailGenerator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public String handleRateLimitException(RateLimitExceededException ex, Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(429); // Set status code to 429 Too Many Requests
        
        model.addAttribute("errorMessage", ex.getMessage());
        
        // Preserve user input
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setEmailContent(request.getParameter("emailContent"));
        
        String tone = request.getParameter("tone");
        emailRequest.setTone(tone != null && !tone.isBlank() ? tone : "Professional");
        
        model.addAttribute("emailRequest", emailRequest);
        
        return "index"; // Return the same UI page, which will display the errorMessage
    }
}
