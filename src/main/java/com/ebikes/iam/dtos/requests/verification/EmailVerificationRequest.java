package com.ebikes.iam.dtos.requests.verification;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email)
    implements Serializable {}
