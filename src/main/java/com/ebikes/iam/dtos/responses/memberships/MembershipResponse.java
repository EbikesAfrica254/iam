package com.ebikes.iam.dtos.responses.memberships;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MembershipResponse(
    UUID id,
    String branchId,
    String branchName,
    Boolean isPrimary,
    String keycloakGroupPath,
    String keycloakUserId,
    String organizationId,
    String organizationName,
    Set<String> roles,
    UUID userExtensionId)
    implements Serializable {}
