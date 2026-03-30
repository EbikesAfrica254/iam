package com.ebikes.iam.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.constants.EventConstants;
import com.ebikes.iam.constants.EventConstants.ExternalContracts;
import com.ebikes.iam.dtos.events.incoming.OrganizationCreatedEvent;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.services.events.InboxService;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.context.EventContext;

import tools.jackson.databind.ObjectMapper;

@DisplayName("OrganizationCreatedEventListener")
@ExtendWith(MockitoExtension.class)
class OrganizationCreatedEventListenerTest {

  private static final byte[] PAYLOAD = "{}".getBytes();
  private static final String SERVICE_REFERENCE = "ref-123";
  private static final String ORGANIZATION_ID = "org-1";
  private static final String OWNER_ID = "owner-1";
  private static final String DISPLAY_NAME = "Test Org";
  private static final String EVENT_TYPE = "organizations.organization.created";
  private static final String SOURCE_SERVICE = "organizations";

  @Mock private InboxService inboxService;
  @Mock private KeycloakGroupAdapter keycloakGroupAdapter;
  @Mock private MembershipService membershipService;
  @Mock private ObjectMapper objectMapper;

  private OrganizationCreatedEventListener listener;
  private OrganizationCreatedEvent event;

  @BeforeEach
  void setUp() {
    listener =
        new OrganizationCreatedEventListener(
            inboxService, keycloakGroupAdapter, membershipService, objectMapper);
    event =
        new OrganizationCreatedEvent(DISPLAY_NAME, ORGANIZATION_ID, OWNER_ID, SERVICE_REFERENCE);
  }

  @Test
  @DisplayName("matches should return true for ORGANIZATIONS_ORGANIZATION_CREATED routing key")
  void matchesShouldReturnTrue() {
    assertThat(
            listener.matches(EventConstants.ExternalContracts.ORGANIZATIONS_ORGANIZATION_CREATED))
        .isTrue();
  }

  @Test
  @DisplayName("matches should return false for unrelated routing key")
  void matchesShouldReturnFalse() {
    assertThat(listener.matches("orders.manifest.contacts")).isFalse();
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @BeforeEach
    void setUp() {
      when(objectMapper.readValue(PAYLOAD, OrganizationCreatedEvent.class)).thenReturn(event);
    }

    @AfterEach
    void tearDown() {
      EventContext.clear();
    }

    @Test
    @DisplayName("should skip processing when EventContext is absent")
    void shouldSkipWhenEventContextAbsent() {
      listener.handle(PAYLOAD);

      verify(inboxService, never()).receive(anyString(), anyString(), anyString());
      verify(keycloakGroupAdapter, never()).create(anyString(), anyString());
      verify(membershipService, never()).create(anyString(), any());
    }

    @Test
    @DisplayName("should skip processing when inbox rejects duplicate event")
    void shouldSkipWhenInboxRejectsDuplicate() {
      EventContext.set(
          "corr-1",
          EVENT_TYPE,
          ExternalContracts.ORGANIZATIONS_ORGANIZATION_CREATED,
          SOURCE_SERVICE);
      when(inboxService.receive(anyString(), anyString(), anyString())).thenReturn(false);

      listener.handle(PAYLOAD);

      verify(keycloakGroupAdapter, never()).create(anyString(), anyString());
      verify(membershipService, never()).create(anyString(), any());
      verify(inboxService, never()).markProcessed(anyString());
    }

    @Test
    @DisplayName("should create Keycloak group, membership and mark inbox processed on success")
    void shouldProcessSuccessfully() {
      EventContext.set(
          "corr-1",
          EVENT_TYPE,
          ExternalContracts.ORGANIZATIONS_ORGANIZATION_CREATED,
          SOURCE_SERVICE);
      when(inboxService.receive(anyString(), anyString(), anyString())).thenReturn(true);

      listener.handle(PAYLOAD);

      verify(keycloakGroupAdapter).create(ORGANIZATION_ID, DISPLAY_NAME);
      verify(membershipService)
          .create(
              OWNER_ID,
              new CreateMembershipRequest(
                  null, false, ORGANIZATION_ID, Set.of(UserRole.ORGANIZATION_ADMIN)));
      verify(inboxService).markProcessed(SERVICE_REFERENCE);
    }

    @Test
    @DisplayName("should swallow exception and not mark inbox processed")
    void shouldSwallowExceptionAndNotMarkProcessed() {
      EventContext.set(
          "corr-1",
          EVENT_TYPE,
          ExternalContracts.ORGANIZATIONS_ORGANIZATION_CREATED,
          SOURCE_SERVICE);
      when(inboxService.receive(anyString(), anyString(), anyString())).thenReturn(true);
      doThrow(new RuntimeException("keycloak unavailable"))
          .when(keycloakGroupAdapter)
          .create(anyString(), anyString());

      listener.handle(PAYLOAD);

      verify(inboxService, never()).markProcessed(anyString());
    }
  }
}
