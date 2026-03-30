package com.ebikes.iam.publishers;

import org.springframework.stereotype.Component;

import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.UserConfigurationRequestedEvent;
import com.ebikes.iam.services.events.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserConfigurationEventPublisher {

  private final OutboxService outboxService;

  public void publishRequest(UserExtension userExtension, String organizationId) {
    UserConfigurationRequestedEvent event =
        new UserConfigurationRequestedEvent(
            DomainEvents.Configuration.REQUESTED,
            userExtension.getKeycloakUserId(),
            organizationId,
            null,
            null);

    outboxService.save(event.eventType(), event, RoutingKeys.IAM_USER_CONFIGURATION);

    log.info(
        "User configuration event published - keycloakUserId={} organizationId={}",
        userExtension.getKeycloakUserId(),
        organizationId);
  }
}
