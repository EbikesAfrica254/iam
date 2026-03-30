package com.ebikes.iam.services.notifications;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ebikes.iam.constants.EventConstants.Source;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequestEvent;
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

  private static final String VERIFICATION_LINK_KEY = "verificationLink";

  private final NotificationEventPublisher notificationEventPublisher;
  private final NotificationMapper notificationMapper;
  private final TokenService tokenService;
  private final VerificationLinkBuilder verificationLinkBuilder;

  public void sendAccountVerification(String organizationId, UserExtension userExtension) {
    GeneratedToken token =
        tokenService.generateToken(TokenType.ACCOUNT_ACTIVATION, userExtension.getId());

    Map<String, Serializable> variables = baseTokenVariables(organizationId);
    variables.put("expirationTime", token.expiresAt().toString());
    variables.put("username", userExtension.getUsername());
    variables.put(
        VERIFICATION_LINK_KEY,
        verificationLinkBuilder.buildAccountActivationLink(token.plainToken()));

    NotificationRequestEvent event =
        notificationMapper.toAccountVerificationRequest(
            userExtension, Map.copyOf(variables), Source.serviceReference());

    notificationEventPublisher.publish(event);

    log.info(
        "Account verification notification sent - email={} organizationId={} userId={}",
        userExtension.getEmail(),
        organizationId,
        userExtension.getId());
  }

  public void sendEmailVerification(String organizationId, UserExtension userExtension) {
    GeneratedToken token = tokenService.generateToken(TokenType.EMAIL_OTP, userExtension.getId());

    Map<String, Serializable> variables = baseTokenVariables(organizationId);
    variables.put(
        VERIFICATION_LINK_KEY,
        verificationLinkBuilder.buildEmailVerificationLink(token.plainToken()));

    NotificationRequestEvent event =
        notificationMapper.toEmailVerificationRequest(
            userExtension, Map.copyOf(variables), Source.serviceReference());

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

    Map<String, Serializable> variables = baseTokenVariables(organizationId);
    variables.put(
        VERIFICATION_LINK_KEY, verificationLinkBuilder.buildPasswordResetLink(token.plainToken()));

    NotificationRequestEvent event =
        notificationMapper.toPasswordResetRequest(
            userExtension, Map.copyOf(variables), Source.serviceReference());

    notificationEventPublisher.publish(event);

    log.info(
        "Password reset notification sent - email={} organizationId={} userId={}",
        userExtension.getEmail(),
        organizationId,
        userExtension.getId());
  }

  public void sendPhoneVerification(String organizationId, UserExtension userExtension) {
    GeneratedToken token = tokenService.generateToken(TokenType.SMS_OTP, userExtension.getId());

    Map<String, Serializable> variables = baseTokenVariables(organizationId);
    variables.put("username", userExtension.getUsername());
    variables.put("verificationCode", token.plainToken());
    variables.put("verificationExpiry", token.expiresAt().toString());

    NotificationRequestEvent event =
        notificationMapper.toPhoneVerificationRequest(
            userExtension, Map.copyOf(variables), Source.serviceReference());

    notificationEventPublisher.publish(event);

    log.info(
        "Phone verification notification sent - organizationId={} phoneNumber={} userId={}",
        organizationId,
        userExtension.getPhoneNumber(),
        userExtension.getId());
  }

  private Map<String, Serializable> baseTokenVariables(String organizationId) {
    Map<String, Serializable> variables = new HashMap<>();
    variables.put("organizationId", organizationId);
    return variables;
  }
}
