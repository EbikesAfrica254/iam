package com.ebikes.iam.dtos.requests.verification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record EmailVerificationRequest(
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email)
    implements Serializable {}
