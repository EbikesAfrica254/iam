package com.ebikes.iam.dtos.requests.verification;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record CompleteEmailVerificationRequest(@NotBlank(message = "Token is required") String code)
        implements Serializable {
}
