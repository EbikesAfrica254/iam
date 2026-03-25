package com.ebikes.iam.dtos.events.incoming;

import com.ebikes.iam.enums.AuditOutcome;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrganizationApprovedAuditEvent(
    UUID entityId,
    String entityType,
    String eventType,
    String failureReason,
    String ipAddress,
    Map<String, String> metadata,
    String organizationId,
    AuditOutcome outcome,
    String serviceReference,
    Instant timestamp,
    String userId)
    implements Serializable {
  public OrganizationApprovedAuditEvent {
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
