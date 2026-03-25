package com.ebikes.iam.support.audit;

import java.util.Map;
import java.util.UUID;

public record AuditContext(
        UUID entityId,
        String entityType,
        String eventType,
        Map<String, String> metadata,
        String organizationId,
        String routingKey) {

    public AuditContext {
        metadata = metadata != null ? Map.copyOf(metadata) : null;
    }
}
