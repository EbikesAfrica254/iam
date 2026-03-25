package com.ebikes.iam.dtos.responses.users;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.iam.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserExtensionDetailResponse(
    String branchId,
    String countryCode,
    OffsetDateTime createdAt,
    OffsetDateTime deletedAt,
    String email,
    boolean emailVerified,
    String firstName,
    UUID id,
    String keycloakUserId,
    String lastName,
    String organizationId,
    String phoneNumber,
    boolean phoneNumberVerified,
    UserStatus status,
    OffsetDateTime updatedAt,
    String username)
    implements Serializable {}
