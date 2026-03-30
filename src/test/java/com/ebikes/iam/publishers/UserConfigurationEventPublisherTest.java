package com.ebikes.iam.publishers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.UserConfigurationRequestedEvent;
import com.ebikes.iam.services.events.OutboxService;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

@DisplayName("UserConfigurationEventPublisher")
@ExtendWith(MockitoExtension.class)
class UserConfigurationEventPublisherTest {

  @Mock private OutboxService outboxService;

  private UserConfigurationEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new UserConfigurationEventPublisher(outboxService);
  }

  @Test
  @DisplayName("should save user configuration event to outbox with correct routing key")
  void shouldSaveWithCorrectRoutingKey() {
    UserExtension user = UserExtensionFixtures.active();
    String organizationId = UUID.randomUUID().toString();

    publisher.publishRequest(user, organizationId);

    verify(outboxService)
        .save(
            eq(DomainEvents.Configuration.REQUESTED),
            any(UserConfigurationRequestedEvent.class),
            eq(RoutingKeys.IAM_USER_CONFIGURATION));
  }

  @Test
  @DisplayName("should include keycloakUserId and organizationId in the event")
  void shouldIncludeCorrectFieldsInEvent() {
    UserExtension user = UserExtensionFixtures.active();
    String organizationId = UUID.randomUUID().toString();
    ArgumentCaptor<UserConfigurationRequestedEvent> captor =
        ArgumentCaptor.forClass(UserConfigurationRequestedEvent.class);

    publisher.publishRequest(user, organizationId);

    verify(outboxService).save(any(), captor.capture(), any());
    UserConfigurationRequestedEvent event = captor.getValue();
    assertThat(event.keycloakUserId()).isEqualTo(user.getKeycloakUserId());
    assertThat(event.organizationId()).isEqualTo(organizationId);
    assertThat(event.eventType()).isEqualTo(DomainEvents.Configuration.REQUESTED);
    assertThat(event.serviceReference()).isNotBlank();
    assertThat(event.timestamp()).isNotNull();
  }
}
