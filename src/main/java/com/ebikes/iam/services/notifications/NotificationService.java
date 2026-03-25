package com.ebikes.iam.services.notifications;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ebikes.iam.constants.EventConstants.EventSource;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequest;
import com.ebikes.iam.dtos.internal.GeneratedToken;
import com.ebikes.iam.enums.TokenType;
import com.ebikes.iam.mappers.NotificationMapper;
import com.ebikes.iam.publishers.NotificationEventPublisher;
import com.ebikes.iam.services.tokens.TokenService;
import com.ebikes.iam.services.verification.VerificationLinkBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

  private final NotificationEventPublisher notificationEventPublisher;
  private final NotificationMapper notificationMapper;
  private final TokenService tokenService;
  private final VerificationLinkBuilder verificationLinkBuilder;

  public void sendAccountVerification(
      String organizationId, String organizationName, UserExtension userExtension) {
    GeneratedToken token =
        tokenService.generateToken(TokenType.ACCOUNT_ACTIVATION, userExtension.getId());

    Map<String, Serializable> variables = baseTokenVariables(organizationId, organizationName);
    variables.put("currentYear", String.valueOf(OffsetDateTime.now().getYear())); // String, not int
    variables.put("expirationTime", token.expiresAt().toString());
    variables.put("logoUrl", "https://placehold.co/400");
    variables.put("organizationAddress", "Nairobi, Kenya");
    variables.put("supportUrl", "https://www.ebikes.co.ke");
    variables.put("username", userExtension.getUsername());
    variables.put(
        "verificationLink", verificationLinkBuilder.buildEmailVerificationLink(token.plainToken()));

    NotificationRequest event =
        notificationMapper.toAccountVerificationRequest(
            userExtension, Map.copyOf(variables), EventSource.serviceReference());

    notificationEventPublisher.publish(event);

    log.info(
        "Account verification notification sent - email={} organizationId={} userId={}",
        userExtension.getEmail(),
        organizationId,
        userExtension.getId());
  }

  public void sendEmailVerification(
      String organizationId, String organizationName, UserExtension userExtension) {
    GeneratedToken token = tokenService.generateToken(TokenType.EMAIL_OTP, userExtension.getId());

    Map<String, Serializable> variables = baseTokenVariables(organizationId, organizationName);
    variables.put(
        "verificationLink", verificationLinkBuilder.buildEmailVerificationLink(token.plainToken()));

    NotificationRequest event =
        notificationMapper.toEmailVerificationRequest(
            userExtension, Map.copyOf(variables), EventSource.serviceReference());

    notificationEventPublisher.publish(event);

    log.info(
        "Email verification notification sent - email={} organizationId={} userId={}",
        userExtension.getEmail(),
        organizationId,
        userExtension.getId());
  }

  public void sendPasswordReset(String organizationId, UserExtension userExtension) {
    GeneratedToken token =
        tokenService.generateToken(TokenType.PASSWORD_RESET, userExtension.getId());

    Map<String, Serializable> variables = baseTokenVariables(organizationId, null);
    variables.put("resetLink", verificationLinkBuilder.buildPasswordResetLink(token.plainToken()));

    NotificationRequest event =
        notificationMapper.toPasswordResetRequest(
            userExtension, Map.copyOf(variables), EventSource.serviceReference());

    notificationEventPublisher.publish(event);

    log.info(
        "Password reset notification sent - email={} organizationId={} userId={}",
        userExtension.getEmail(),
        organizationId,
        userExtension.getId());
  }

  public void sendPhoneVerification(
      String organizationId, String organizationName, UserExtension userExtension) {
    GeneratedToken token = tokenService.generateToken(TokenType.SMS_OTP, userExtension.getId());

    Map<String, Serializable> variables = baseTokenVariables(organizationId, organizationName);
    variables.put("username", userExtension.getUsername());
    variables.put("verificationCode", token.plainToken());
    variables.put("verificationExpiry", token.expiresAt().toString());

    NotificationRequest event =
        notificationMapper.toPhoneVerificationRequest(
            userExtension, Map.copyOf(variables), EventSource.serviceReference());

    notificationEventPublisher.publish(event);

    log.info(
        "Phone verification notification sent - organizationId={} phoneNumber={} userId={}",
        organizationId,
        userExtension.getPhoneNumber(),
        userExtension.getId());
  }

  private Map<String, Serializable> baseTokenVariables(
      String organizationId, String organizationName) {
    Map<String, Serializable> variables = new HashMap<>();
    variables.put("organizationId", organizationId);
    if (organizationName != null) {
      variables.put("organizationName", organizationName);
    }
    return variables;
  }
}
