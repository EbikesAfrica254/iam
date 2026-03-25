package com.ebikes.iam.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.dtos.events.incoming.BatchContactsEvent;
import com.ebikes.iam.services.contacts.ContactService;
import com.ebikes.iam.services.events.InboxService;
import com.ebikes.iam.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsListener implements IncomingEventHandler {

  private final ContactService contactService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(byte[] payload) {
    BatchContactsEvent event = objectMapper.readValue(payload, BatchContactsEvent.class);
    log.info("Received BatchContactsEvent: documentId={}", event.documentId());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      contactService.processContacts(event);
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error("Failed to process contacts for documentId={}", event.documentId(), e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.equals(RoutingKeys.ORDERS_MANIFEST_CONTACTS);
  }
}
