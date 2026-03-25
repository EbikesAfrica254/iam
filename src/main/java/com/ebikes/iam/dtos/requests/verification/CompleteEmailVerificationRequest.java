package com.ebikes.iam.dtos.requests.verification;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

public record CompleteEmailVerificationRequest(@NotBlank(message = "Token is required") String code)
    implements Serializable {}
