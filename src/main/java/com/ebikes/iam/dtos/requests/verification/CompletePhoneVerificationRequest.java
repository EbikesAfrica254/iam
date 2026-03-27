package com.ebikes.iam.dtos.requests.verification;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

public record CompletePhoneVerificationRequest(@NotBlank(message = "Code is required") String code)
    implements Serializable {}
