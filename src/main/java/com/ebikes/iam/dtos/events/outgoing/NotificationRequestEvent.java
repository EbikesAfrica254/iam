package com.ebikes.iam.dtos.events.outgoing;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.ebikes.iam.enums.ChannelType;
import com.ebikes.iam.enums.NotificationCategory;

public record NotificationRequestEvent(
    String branchId,
    @NotNull NotificationCategory category,
    @NotNull ChannelType channel,
    @NotBlank String eventType,
    @NotBlank String organizationId,
    @NotBlank String recipient,
    @NotBlank String serviceReference,
    String subjectUserId,
    @Pattern(
            regexp = "^[A-Z][A-Z0-9_]{2,99}$",
            message = "Template name must be SCREAMING_SNAKE_CASE")
        String templateName,
    Instant timestamp,
    Map<String, Object> variables)
    implements Serializable {

  public NotificationRequestEvent {
    timestamp = timestamp != null ? timestamp : Instant.now();
    variables = variables != null ? Map.copyOf(variables) : Map.of();
  }
}
