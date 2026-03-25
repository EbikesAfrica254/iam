package com.ebikes.iam.dtos.responses.users;

import com.ebikes.iam.enums.UserStatus;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserExtensionSummaryResponse(
    OffsetDateTime createdAt,
    String email,
    String firstName,
    UUID id,
    String lastName,
    String phoneNumber,
    UserStatus status,
    OffsetDateTime updatedAt,
    String username)
    implements Serializable {}
