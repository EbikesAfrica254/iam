package com.ebikes.iam.dtos.requests.users;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.iam.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateUserExtensionRequest(
    @Email(message = "Email must be valid") String email,
    boolean emailVerified,
    @Size(max = 255) String firstName,
    @Size(max = 255) String lastName,
    UUID organizationId,
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid E.164 format") @Size(max = 20) String phoneNumber,
    boolean phoneNumberVerified,
    UserStatus status) {}
