package com.ebikes.iam.support.audit;

import java.util.UUID;
import java.util.function.Function;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.constants.MDCKeys;
import com.ebikes.iam.dtos.events.outgoing.AuditEvent;
import com.ebikes.iam.enums.AuditOutcome;
import com.ebikes.iam.publishers.AuditEventPublisher;
import com.ebikes.iam.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Component
@RequiredArgsConstructor
public class AuditTemplate {

  private final AuditEventPublisher auditEventPublisher;

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
    return execute(context, operation, null);
  }

  @SneakyThrows
  public <T, E extends Exception> T execute(
      AuditContext context, ThrowingSupplier<T, E> operation, Function<T, UUID> entityIdExtractor) {
    try {
      T result = operation.get();
      UUID extractedId = entityIdExtractor != null ? entityIdExtractor.apply(result) : null;
      UUID entityId = context.entityId() != null ? context.entityId() : extractedId;
      auditEventPublisher.publishSuccess(
          buildEvent(context, entityId, AuditOutcome.SUCCESS, null), context.routingKey());
      return result;
    } catch (Exception e) {
      auditEventPublisher.publishFailure(
          buildEvent(context, context.entityId(), AuditOutcome.FAILURE, e.getMessage()),
          context.routingKey());
      throw e;
    }
  }

  private AuditEvent buildEvent(
      AuditContext context, UUID entityId, AuditOutcome outcome, String failureReason) {
    String actorId =
        switch (ExecutionContext.get()) {
          case ExecutionContext.UserContext uc -> uc.userId();
          case ExecutionContext.SystemContext ignored -> ApplicationConstants.SYSTEM_ID;
        };

    return new AuditEvent(
        entityId,
        context.entityType(),
        context.eventType(),
        failureReason,
        MDC.get(MDCKeys.IP_ADDRESS),
        context.metadata(),
        context.organizationId(),
        outcome,
        null,
        null,
        actorId);
  }
}
