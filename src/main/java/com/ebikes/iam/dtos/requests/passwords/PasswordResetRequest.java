package com.ebikes.iam.dtos.requests.passwords;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {}
