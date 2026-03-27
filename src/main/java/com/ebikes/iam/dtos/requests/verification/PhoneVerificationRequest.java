package com.ebikes.iam.dtos.requests.verification;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

public record PhoneVerificationRequest(
    @NotBlank(message = "Phone number is required") String phoneNumber) implements Serializable {}
