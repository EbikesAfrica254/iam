package com.ebikes.iam.dtos.responses.contacts;

import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContactResponse(
    UUID id,
    String branchId,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    String organizationId,
    String phoneNumber,
    String sourceReference,
    ContactSourceType sourceType,
    ContactStatus status,
    UUID userExtensionId)
    implements Serializable {}
