package com.ebikes.iam.publishers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.configurations.properties.NotificationProperties;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequestEvent;
import com.ebikes.iam.services.events.OutboxService;
import com.ebikes.iam.support.fixtures.NotificationRequestFixtures;

@DisplayName("NotificationEventPublisher")
@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

  @Mock private OutboxService outboxService;
  @Mock private NotificationProperties notificationProperties;

  private NotificationEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new NotificationEventPublisher(outboxService, notificationProperties);
  }

  @Nested
  @DisplayName("when notifications are enabled")
  class WhenEnabled {

    @BeforeEach
    void setUp() {
      when(notificationProperties.isEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("should save email notification to outbox with EMAIL routing key")
    void shouldSaveEmailNotificationWithCorrectRoutingKey() {
      NotificationRequestEvent request = NotificationRequestFixtures.emailVerification();

      publisher.publish(request);

      verify(outboxService).save(request.eventType(), request, RoutingKeys.NOTIFICATIONS_EMAIL);
    }

    @Test
    @DisplayName("should save SMS notification to outbox with SMS routing key")
    void shouldSaveSmsNotificationWithCorrectRoutingKey() {
      NotificationRequestEvent request = NotificationRequestFixtures.phoneVerification();

      publisher.publish(request);

      verify(outboxService).save(request.eventType(), request, RoutingKeys.NOTIFICATIONS_SMS);
    }
  }

  @Nested
  @DisplayName("when notifications are disabled")
  class WhenDisabled {

    @Test
    @DisplayName("should not save to outbox")
    void shouldNotSaveToOutbox() {
      when(notificationProperties.isEnabled()).thenReturn(false);

      publisher.publish(NotificationRequestFixtures.accountVerification());

      verifyNoInteractions(outboxService);
    }
  }
}
