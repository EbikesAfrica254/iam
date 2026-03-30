package com.ebikes.iam.listeners;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.constants.EventConstants.ExternalContracts;
import com.ebikes.iam.dtos.events.incoming.OrganizationCreatedEvent;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.services.events.InboxService;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationCreatedEventListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final KeycloakGroupAdapter keycloakGroupAdapter;
  private final MembershipService membershipService;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(byte[] payload) {
    OrganizationCreatedEvent event =
        objectMapper.readValue(payload, OrganizationCreatedEvent.class);
    log.info("Received OrganizationCreatedEvent: serviceReference={}", event.serviceReference());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      keycloakGroupAdapter.create(event.organizationId(), event.displayName());

      CreateMembershipRequest request =
          new CreateMembershipRequest(
              null, false, event.organizationId(), Set.of(UserRole.ORGANIZATION_ADMIN));

      membershipService.create(event.ownerId(), request);
      inboxService.markProcessed(event.serviceReference());

    } catch (Exception e) {
      log.error(
          "Failed to process OrganizationCreatedEvent: serviceReference={}",
          event.serviceReference(),
          e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.equals(ExternalContracts.ORGANIZATIONS_ORGANIZATION_CREATED);
  }
}
