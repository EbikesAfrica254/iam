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

import com.ebikes.iam.constants.EventConstants.ExternalContracts;
import com.ebikes.iam.dtos.events.incoming.BatchContactsEvent;
import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.services.contacts.ContactService;
import com.ebikes.iam.services.events.InboxService;
import com.ebikes.iam.support.context.EventContext;

import tools.jackson.databind.ObjectMapper;

@DisplayName("OrderEventsListener")
@ExtendWith(MockitoExtension.class)
class OrderEventsListenerTest {

  private static final byte[] PAYLOAD = "{}".getBytes();
  private static final String SERVICE_REFERENCE = "ref-123";
  private static final String EVENT_TYPE = "orders.manifest.contacts";
  private static final String SOURCE_SERVICE = "orders";

  @Mock private ContactService contactService;
  @Mock private InboxService inboxService;
  @Mock private ObjectMapper objectMapper;

  private OrderEventsListener listener;
  private BatchContactsEvent event;

  @BeforeEach
  void setUp() {
    listener = new OrderEventsListener(contactService, inboxService, objectMapper);
    event =
        new BatchContactsEvent(
            "branch-1",
            "doc-1",
            "org-1",
            Set.of("+254700000001"),
            SERVICE_REFERENCE,
            ContactSourceType.CSV_MANIFEST);
  }

  @Test
  @DisplayName("matches should return true for ORDERS_MANIFEST_CONTACTS routing key")
  void matchesShouldReturnTrueForOrdersManifestContacts() {
    assertThat(listener.matches(ExternalContracts.ORDERS_MANIFEST_CONTACTS)).isTrue();
  }

  @Test
  @DisplayName("matches should return false for unrelated routing key")
  void matchesShouldReturnFalseForUnrelatedRoutingKey() {
    assertThat(listener.matches("iam.user-extension.created")).isFalse();
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @BeforeEach
    void setUp() {
      when(objectMapper.readValue(PAYLOAD, BatchContactsEvent.class)).thenReturn(event);
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
      verify(contactService, never()).processContacts(any());
    }

    @Test
    @DisplayName("should skip processing when inbox rejects duplicate event")
    void shouldSkipWhenInboxRejectsDuplicate() {
      EventContext.set(
          "corr-1", EVENT_TYPE, ExternalContracts.ORDERS_MANIFEST_CONTACTS, SOURCE_SERVICE);
      when(inboxService.receive(anyString(), anyString(), anyString())).thenReturn(false);

      listener.handle(PAYLOAD);

      verify(contactService, never()).processContacts(any());
      verify(inboxService, never()).markProcessed(anyString());
    }

    @Test
    @DisplayName("should process contacts and mark inbox processed on success")
    void shouldProcessContactsAndMarkProcessed() {
      EventContext.set(
          "corr-1", EVENT_TYPE, ExternalContracts.ORDERS_MANIFEST_CONTACTS, SOURCE_SERVICE);
      when(inboxService.receive(anyString(), anyString(), anyString())).thenReturn(true);

      listener.handle(PAYLOAD);

      verify(contactService).processContacts(event);
      verify(inboxService).markProcessed(SERVICE_REFERENCE);
    }

    @Test
    @DisplayName("should swallow exception from contactService and not mark inbox processed")
    void shouldSwallowContactServiceException() {
      EventContext.set(
          "corr-1", EVENT_TYPE, ExternalContracts.ORDERS_MANIFEST_CONTACTS, SOURCE_SERVICE);
      when(inboxService.receive(anyString(), anyString(), anyString())).thenReturn(true);
      doThrow(new RuntimeException("processing failed"))
          .when(contactService)
          .processContacts(any());

      listener.handle(PAYLOAD);

      verify(inboxService, never()).markProcessed(anyString());
    }
  }
}
