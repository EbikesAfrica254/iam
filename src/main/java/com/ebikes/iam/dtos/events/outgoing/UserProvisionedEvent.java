package com.ebikes.iam.dtos.events.outgoing;

import com.ebikes.iam.constants.EventConstants;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.Instant;

public record UserProvisionedEvent(
        @NotBlank String keycloakUserId,
        @NotBlank String organizationId,
        String branchId,
        String email,
        @NotBlank String eventType,
        String serviceReference,
        Instant timestamp)
        implements Serializable {

    public UserProvisionedEvent {
        branchId = branchId != null && !branchId.isBlank() ? branchId : null;
        serviceReference =
                serviceReference != null ? serviceReference : EventConstants.EventSource.serviceReference();
        timestamp = timestamp != null ? timestamp : Instant.now();
    }
}
