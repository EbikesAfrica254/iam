package com.ebikes.iam.dtos.requests.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record CompleteAccountActivationRequest(
    @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
    @NotBlank(message = "Token is required") String token)
    implements Serializable {}
