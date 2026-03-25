package com.ebikes.iam.listeners;

import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.dtos.events.incoming.OrganizationApprovedAuditEvent;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.mappers.MembershipMapper;
import com.ebikes.iam.services.events.InboxService;
import com.ebikes.iam.services.keycloak.groups.KeycloakGroupService;
import com.ebikes.iam.services.users.MembershipService;
import com.ebikes.iam.support.context.EventContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationEventListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final KeycloakGroupService keycloakGroupService;
  private final MembershipMapper membershipMapper;
  private final MembershipService membershipService;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(byte[] payload) {
    OrganizationApprovedAuditEvent event =
        objectMapper.readValue(payload, OrganizationApprovedAuditEvent.class);
    log.info(
        "Received OrganizationApprovedAuditEvent: serviceReference={}", event.serviceReference());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      keycloakGroupService.create(event.organizationId(), event.metadata().get("displayName"));
      CreateMembershipRequest request =
          membershipMapper.toRequest(event, false, Set.of(UserRole.ORGANIZATION_ADMIN));
      membershipService.create(event.metadata().get("ownerId"), request);
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error(
          "Failed to process organization approved event for serviceReference={}",
          event.serviceReference(),
          e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.equals(RoutingKeys.ORGANIZATIONS_APPROVED);
  }
}
