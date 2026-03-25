package com.ebikes.iam.support.audit;

import com.ebikes.iam.constants.MDCKeys;
import com.ebikes.iam.dtos.events.outgoing.AuditEvent;
import com.ebikes.iam.enums.AuditOutcome;
import com.ebikes.iam.publishers.AuditEventPublisher;
import com.ebikes.iam.support.context.ExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditTemplate {

  private final AuditEventPublisher auditEventPublisher;

  private AuditEvent buildEvent(AuditContext context, AuditOutcome outcome, String failureReason) {
    return new AuditEvent(
        context.entityId(),
        context.entityType(),
        context.eventType(),
        failureReason,
        MDC.get(MDCKeys.IP_ADDRESS),
        context.metadata(),
        context.organizationId(),
        outcome,
        null,
        null,
        ExecutionContext.getUserId());
  }

  public <E extends Exception> void execute(AuditContext context, ThrowingRunnable<E> operation) {
    execute(
        context,
        () -> {
          operation.run();
          return null;
        });
  }

  @SneakyThrows
  public <T, E extends Exception> T execute(
      AuditContext context, ThrowingSupplier<T, E> operation) {
    try {
      T result = operation.get();
      auditEventPublisher.publishSuccess(
          buildEvent(context, AuditOutcome.SUCCESS, null), context.routingKey());
      return result;
    } catch (Exception e) {
      auditEventPublisher.publishFailure(
          buildEvent(context, AuditOutcome.FAILURE, e.getMessage()), context.routingKey());
      throw e;
    }
  }
}
