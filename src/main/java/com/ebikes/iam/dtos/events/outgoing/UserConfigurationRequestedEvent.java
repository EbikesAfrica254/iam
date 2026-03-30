package com.ebikes.iam.dtos.events.outgoing;

import java.io.Serializable;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

import com.ebikes.iam.constants.EventConstants.Source;

public record UserConfigurationRequestedEvent(
    @NotBlank String eventType,
    @NotBlank String keycloakUserId,
    @NotBlank String organizationId,
    String serviceReference,
    Instant timestamp)
    implements Serializable {

  public UserConfigurationRequestedEvent {
    serviceReference = serviceReference != null ? serviceReference : Source.serviceReference();
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
