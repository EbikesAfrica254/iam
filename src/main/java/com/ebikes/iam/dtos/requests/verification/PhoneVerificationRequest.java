package com.ebikes.iam.dtos.requests.verification;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record PhoneVerificationRequest(
        @NotBlank(message = "Phone number is required") String phoneNumber) implements Serializable {
}
