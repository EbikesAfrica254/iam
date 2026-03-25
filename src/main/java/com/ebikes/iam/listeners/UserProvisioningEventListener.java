package com.ebikes.iam.listeners;

import com.ebikes.iam.dtos.internal.UserProvisionedApplicationEvent;
import com.ebikes.iam.services.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningEventListener {

  private final NotificationService notificationService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onUserProvisioned(UserProvisionedApplicationEvent event) {
    log.info(
        "Handling post-commit provisioning notifications - userId={} organizationId={}",
        event.userExtension().getId(),
        event.organizationId());

    sendAccountVerification(event);
    sendPhoneVerification(event);
  }

  private void sendAccountVerification(UserProvisionedApplicationEvent event) {
    try {
      notificationService.sendAccountVerification(
          event.organizationId(), event.organizationName(), event.userExtension());
    } catch (Exception e) {
      log.error(
          "Failed to send account verification - userId={} organizationId={}",
          event.userExtension().getId(),
          event.organizationId(),
          e);
    }
  }

  private void sendPhoneVerification(UserProvisionedApplicationEvent event) {
    try {
      notificationService.sendPhoneVerification(
          event.organizationId(), event.organizationName(), event.userExtension());
    } catch (Exception e) {
      log.error(
          "Failed to send phone verification - userId={} organizationId={}",
          event.userExtension().getId(),
          event.organizationId(),
          e);
    }
  }
}
