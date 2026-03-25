package com.ebikes.iam.publishers;

import com.ebikes.iam.dtos.events.outgoing.AuditEvent;
import com.ebikes.iam.services.events.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

  private final OutboxService outboxService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void publishFailure(AuditEvent event, String routingKey) {
    outboxService.save(event.eventType(), event, routingKey);
  }

  public void publishSuccess(AuditEvent event, String routingKey) {
    outboxService.save(event.eventType(), event, routingKey);
  }
}
