package com.example.emailGenerator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailRequest {

    @NotBlank(message = "Email content cannot be empty")
    private String emailContent;

    private String tone;
}