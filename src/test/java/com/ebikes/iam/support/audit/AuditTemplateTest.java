package com.ebikes.iam.support.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.dtos.events.outgoing.AuditEvent;
import com.ebikes.iam.publishers.AuditEventPublisher;
import com.ebikes.iam.support.infrastructure.WithExecutionContext;

@DisplayName("AuditTemplate")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class AuditTemplateTest {

  private static final String ROUTING_KEY = "iam.user-extension.audit";

  @Mock private AuditEventPublisher auditEventPublisher;

  @InjectMocks private AuditTemplate auditTemplate;

  private AuditContext context;

  @BeforeEach
  void setUp() {
    context =
        new AuditContext(
            UUID.randomUUID(),
            "USER",
            "iam.user-extension.created",
            null,
            UUID.randomUUID().toString(),
            ROUTING_KEY);
  }

  @Nested
  @DisplayName("execute with ThrowingRunnable")
  class ExecuteWithRunnable {

    @Test
    @DisplayName("should execute the operation")
    void shouldExecuteTheOperation() {
      ThrowingRunnable<Exception> operation = () -> {};

      auditTemplate.execute(context, operation);

      // If operation was not invoked, the success event would not be published —
      // verified by the next test. Here we confirm no exception means it ran.
      verify(auditEventPublisher).publishSuccess(any(AuditEvent.class), eq(ROUTING_KEY));
    }

    @Test
    @DisplayName("should publish a success audit event when the operation succeeds")
    void shouldPublishSuccessEventOnSuccess() {
      auditTemplate.execute(context, () -> {});

      verify(auditEventPublisher).publishSuccess(any(AuditEvent.class), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishFailure(any(), any());
    }

    @Test
    @DisplayName("should publish a failure audit event and rethrow when the operation throws")
    void shouldPublishFailureEventAndRethrowOnException() {
      RuntimeException cause = new RuntimeException("operation failed");

      assertThatThrownBy(
              () ->
                  auditTemplate.execute(
                      context,
                      () -> {
                        throw cause;
                      }))
          .isSameAs(cause);

      verify(auditEventPublisher).publishFailure(any(AuditEvent.class), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishSuccess(any(), any());
    }
  }

  @Nested
  @DisplayName("execute with ThrowingSupplier")
  class ExecuteWithSupplier {

    @Test
    @DisplayName("should return the result of the operation")
    void shouldReturnOperationResult() {
      String result = auditTemplate.execute(context, () -> "expected-result");

      assertThat(result).isEqualTo("expected-result");
    }

    @Test
    @DisplayName("should publish a success audit event when the operation succeeds")
    void shouldPublishSuccessEventOnSuccess() {
      auditTemplate.execute(context, () -> "result");

      verify(auditEventPublisher).publishSuccess(any(AuditEvent.class), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishFailure(any(), any());
    }

    @Test
    @DisplayName("should publish a failure audit event and rethrow when the operation throws")
    void shouldPublishFailureEventAndRethrowOnException() {
      RuntimeException cause = new RuntimeException("supplier failed");

      assertThatThrownBy(
              () ->
                  auditTemplate.execute(
                      context,
                      () -> {
                        throw cause;
                      }))
          .isSameAs(cause);

      verify(auditEventPublisher).publishFailure(any(AuditEvent.class), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishSuccess(any(), any());
    }
  }
}
