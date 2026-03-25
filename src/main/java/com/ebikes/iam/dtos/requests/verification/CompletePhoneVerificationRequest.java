package com.ebikes.iam.dtos.requests.verification;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record CompletePhoneVerificationRequest(@NotBlank(message = "Code is required") String code)
    implements Serializable {}
