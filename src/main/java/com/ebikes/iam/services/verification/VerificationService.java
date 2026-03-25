package com.ebikes.iam.services.verification;

import com.ebikes.iam.constants.EventConstants.EventTypes;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.constants.MDCKeys;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.TokenType;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.services.keycloak.users.KeycloakUserService;
import com.ebikes.iam.services.notifications.NotificationService;
import com.ebikes.iam.services.security.RateLimitService;
import com.ebikes.iam.services.tokens.TokenService;
import com.ebikes.iam.services.users.MembershipService;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditMetadataBuilder;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.security.TokenHashUtilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class VerificationService {

  private static final String ENTITY_TYPE = "USER";

  private static final String RATE_LIMIT_KEY_EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
  private static final String RATE_LIMIT_KEY_PASSWORD_RESET = "PASSWORD_RESET";
  private static final String RATE_LIMIT_KEY_PHONE_VERIFICATION = "PHONE_VERIFICATION";

  private static final VerificationRequestConfig EMAIL_VERIFICATION_CONFIG =
      new VerificationRequestConfig(
          NotificationType.EMAIL_OTP, RATE_LIMIT_KEY_EMAIL_VERIFICATION, true, TokenType.EMAIL_OTP);

  private static final VerificationRequestConfig PASSWORD_RESET_CONFIG =
      new VerificationRequestConfig(
          NotificationType.PASSWORD_RESET,
          RATE_LIMIT_KEY_PASSWORD_RESET,
          false,
          TokenType.PASSWORD_RESET);

  private static final VerificationRequestConfig PHONE_VERIFICATION_CONFIG =
      new VerificationRequestConfig(
          NotificationType.PHONE_OTP, RATE_LIMIT_KEY_PHONE_VERIFICATION, true, TokenType.SMS_OTP);

  private final AuditTemplate auditTemplate;
  private final KeycloakUserService keycloakUserService;
  private final MembershipService membershipService;
  private final NotificationService notificationService;
  private final RateLimitService rateLimitService;
  private final TokenService tokenService;
  private final UserExtensionService userExtensionService;

  @Transactional
  public void completeAccountActivation(String password, String token) {
    String tokenHash = TokenHashUtilities.hashToken(token);
    tokenService.validateToken(tokenHash, TokenType.ACCOUNT_ACTIVATION);

    UUID userExtensionId = tokenService.getUserExtensionIdFromToken(tokenHash);
    UserExtension userExtension = userExtensionService.findUserExtensionById(userExtensionId);

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            ENTITY_TYPE,
            EventTypes.IAM.ACCOUNT_VERIFICATION_COMPLETED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_ACCOUNT_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          keycloakUserService.resetPassword(userExtension.getKeycloakUserId(), password, false);
          keycloakUserService.verifyEmail(userExtension.getKeycloakUserId());
          keycloakUserService.enableUser(userExtension.getKeycloakUserId());
          userExtensionService.activate(userExtension);
          tokenService.consumeToken(tokenHash);
        });

    log.info("Account activation completed - userId={}", userExtension.getId());
  }

  @Transactional
  public void completeEmailVerification(String token) {
    String tokenHash = TokenHashUtilities.hashToken(token);
    tokenService.validateToken(tokenHash, TokenType.EMAIL_OTP);

    UUID userExtensionId = tokenService.getUserExtensionIdFromToken(tokenHash);
    UserExtension userExtension = userExtensionService.findUserExtensionById(userExtensionId);

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            ENTITY_TYPE,
            EventTypes.IAM.EMAIL_VERIFICATION_COMPLETED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_EMAIL_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          userExtensionService.verifyEmail(userExtension);
          tokenService.consumeToken(tokenHash);
        });

    log.info("Email verification completed - userId={}", userExtension.getId());
  }

  @Transactional
  public void completePasswordReset(String newPassword, String token) {
    String tokenHash = TokenHashUtilities.hashToken(token);
    tokenService.validateToken(tokenHash, TokenType.PASSWORD_RESET);

    UUID userExtensionId = tokenService.getUserExtensionIdFromToken(tokenHash);
    UserExtension userExtension = userExtensionService.findUserExtensionById(userExtensionId);

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            ENTITY_TYPE,
            EventTypes.IAM.PASSWORD_RESET_COMPLETED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_PASSWORD_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          keycloakUserService.resetPassword(userExtension.getKeycloakUserId(), newPassword, false);
          tokenService.consumeToken(tokenHash);
        });

    log.info("Password reset completed - userId={}", userExtension.getId());
  }

  @Transactional
  public void completePhoneNumberVerification(String token) {
    String tokenHash = TokenHashUtilities.hashToken(token);
    tokenService.validateToken(tokenHash, TokenType.SMS_OTP);

    UUID userExtensionId = tokenService.getUserExtensionIdFromToken(tokenHash);
    UserExtension userExtension = userExtensionService.findUserExtensionById(userExtensionId);

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            ENTITY_TYPE,
            EventTypes.IAM.PHONE_VERIFICATION_COMPLETED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_PHONE_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          keycloakUserService.verifyPhone(userExtension.getKeycloakUserId());
          tokenService.consumeToken(tokenHash);
          userExtensionService.verifyPhone(userExtension);
        });

    log.info("Phone verification completed - userId={}", userExtension.getId());
  }

  @Transactional
  public void requestEmailVerification(String email) {
    Optional<UserExtension> result = userExtensionService.findUserExtensionByEmail(email);

    if (result.isEmpty()) {
      log.info("Email verification requested for non-existent email.");
      return;
    }

    UserExtension userExtension = result.get();
    processVerificationRequest(userExtension, EMAIL_VERIFICATION_CONFIG);

    log.info("Email verification requested - userId={}.", userExtension.getId());
  }

  @Transactional
  public void requestPasswordReset(String email) {
    Optional<UserExtension> result = userExtensionService.findUserExtensionByEmail(email);

    if (result.isEmpty()) {
      log.info("Password reset requested for non-existent email.");
      return;
    }

    UserExtension userExtension = result.get();
    processVerificationRequest(userExtension, PASSWORD_RESET_CONFIG);

    log.info("Password reset requested - userId={}.", userExtension.getId());
  }

  @Transactional
  public void requestPhoneVerification(String phoneNumber) {
    Optional<UserExtension> result =
        userExtensionService.findUserExtensionByPhoneNumber(phoneNumber);

    if (result.isEmpty()) {
      log.info("Phone verification requested for non-existent phone.");
      return;
    }

    UserExtension userExtension = result.get();
    processVerificationRequest(userExtension, PHONE_VERIFICATION_CONFIG);

    log.info("Phone verification requested - userId={}.", userExtension.getId());
  }

  private void dispatchNotification(
      UserExtension userExtension, NotificationType notificationType) {
    if (notificationType == NotificationType.PASSWORD_RESET) {
      notificationService.sendPasswordReset(userExtension.getOrganizationId(), userExtension);
      return;
    }

    Membership membership = findMembershipOrThrow(userExtension);
    String organizationId = membership.getOrganizationId();
    String organizationName = membership.getOrganizationName();

    switch (notificationType) {
      case ACCOUNT_ACTIVATION ->
          notificationService.sendAccountVerification(
              organizationId, organizationName, userExtension);
      case EMAIL_OTP ->
          notificationService.sendEmailVerification(
              organizationId, organizationName, userExtension);
      case PHONE_OTP ->
          notificationService.sendPhoneVerification(
              organizationId, organizationName, userExtension);
      default ->
          throw new IllegalStateException("Unhandled notification type: " + notificationType);
    }
  }

  private Membership findMembershipOrThrow(UserExtension userExtension) {
    return membershipService
        .findMembershipInScope(
            null, userExtension.getKeycloakUserId(), userExtension.getOrganizationId())
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "No organization-level membership found for user"));
  }

  private void processVerificationRequest(
      UserExtension userExtension, VerificationRequestConfig config) {
    if (config.enforceActiveStatus()) {
      validateUserIsActive(userExtension);
    }

    rateLimitService.checkResendLimit(
        MDCKeys.IP_ADDRESS, config.rateLimitKey(), userExtension.getId().toString());

    tokenService.invalidateUserTokens(config.tokenType(), userExtension.getId());

    dispatchNotification(userExtension, config.notificationType());
  }

  private void validateUserIsActive(UserExtension userExtension) {
    if (userExtension.getStatus() != UserStatus.ACTIVE) {
      log.warn(
          "Verification request failed for inactive user - userId={} status={}",
          userExtension.getId(),
          userExtension.getStatus());
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Cannot process verification request for inactive user",
          "status",
          userExtension.getStatus().name());
    }
  }

  private enum NotificationType {
    ACCOUNT_ACTIVATION,
    EMAIL_OTP,
    PASSWORD_RESET,
    PHONE_OTP
  }

  private record VerificationRequestConfig(
      NotificationType notificationType,
      String rateLimitKey,
      boolean enforceActiveStatus,
      TokenType tokenType) {}
}
