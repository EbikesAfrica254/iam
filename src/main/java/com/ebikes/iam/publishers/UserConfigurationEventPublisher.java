package com.ebikes.iam.publishers;

import org.springframework.stereotype.Component;

import com.ebikes.iam.constants.EventConstants.EventSource;
import com.ebikes.iam.constants.EventConstants.EventTypes;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.UserProvisionedEvent;
import com.ebikes.iam.services.events.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserConfigurationEventPublisher {

  private final OutboxService outboxService;

  public void publishUserProvisioned(
      UserExtension userExtension, String organizationId, String branchId) {
    UserProvisionedEvent event =
        new UserProvisionedEvent(
            userExtension.getKeycloakUserId(),
            organizationId,
            branchId,
            userExtension.getEmail(),
            EventTypes.IAM.USER_PROVISIONED,
            EventSource.serviceReference(),
            null);

    outboxService.save(event.eventType(), event, RoutingKeys.IAM_USER_CONFIGURATION);

    log.info(
        "User configuration event published - keycloakUserId={} organizationId={}",
        userExtension.getKeycloakUserId(),
        organizationId);
  }
}
