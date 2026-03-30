package com.ebikes.iam.services.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequestEvent;
import com.ebikes.iam.dtos.internal.GeneratedToken;
import com.ebikes.iam.enums.TokenType;
import com.ebikes.iam.mappers.NotificationMapper;
import com.ebikes.iam.publishers.NotificationEventPublisher;
import com.ebikes.iam.services.tokens.TokenService;
import com.ebikes.iam.services.verification.VerificationLinkBuilder;
import com.ebikes.iam.support.fixtures.NotificationRequestFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

@DisplayName("NotificationService")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final String PLAIN_TOKEN = "plain-token-abc123";
  private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.now().plusHours(24);

  @Mock private NotificationEventPublisher notificationEventPublisher;
  @Mock private NotificationMapper notificationMapper;
  @Mock private TokenService tokenService;
  @Mock private VerificationLinkBuilder verificationLinkBuilder;

  @InjectMocks private NotificationService notificationService;

  private UserExtension userExtension;
  private GeneratedToken generatedToken;

  @BeforeEach
  void setUp() {
    userExtension = UserExtensionFixtures.active();
    generatedToken = new GeneratedToken(EXPIRES_AT, PLAIN_TOKEN, null);
  }

  @Nested
  @DisplayName("sendAccountVerification")
  class SendAccountVerification {

    private static final String ACTIVATION_LINK =
        "https://app.ebikes.test/activate?token=" + PLAIN_TOKEN;

    private final NotificationRequestEvent mappedRequest =
        NotificationRequestFixtures.accountVerification();

    @Captor private ArgumentCaptor<Map<String, Serializable>> variablesCaptor;

    @BeforeEach
    void setUp() {
      when(tokenService.generateToken(TokenType.ACCOUNT_ACTIVATION, userExtension.getId()))
          .thenReturn(generatedToken);
      when(verificationLinkBuilder.buildAccountActivationLink(PLAIN_TOKEN))
          .thenReturn(ACTIVATION_LINK);
      when(notificationMapper.toAccountVerificationRequest(eq(userExtension), any(), any()))
          .thenReturn(mappedRequest);
    }

    @Test
    @DisplayName("should generate an ACCOUNT_ACTIVATION token for the user")
    void shouldGenerateAccountActivationToken() {
      notificationService.sendAccountVerification(ORGANIZATION_ID, userExtension);

      verify(tokenService).generateToken(TokenType.ACCOUNT_ACTIVATION, userExtension.getId());
    }

    @Test
    @DisplayName("should build an account activation link from the plain token")
    void shouldBuildAccountActivationLink() {
      notificationService.sendAccountVerification(ORGANIZATION_ID, userExtension);

      verify(verificationLinkBuilder).buildAccountActivationLink(PLAIN_TOKEN);
    }

    @Test
    @DisplayName("should pass the correct variables to the notification mapper")
    void shouldPassCorrectVariablesToMapper() {
      notificationService.sendAccountVerification(ORGANIZATION_ID, userExtension);

      verify(notificationMapper)
          .toAccountVerificationRequest(eq(userExtension), variablesCaptor.capture(), any());

      assertThat(variablesCaptor.getValue())
          .containsEntry("verificationLink", ACTIVATION_LINK)
          .containsEntry("expirationTime", EXPIRES_AT.toString())
          .containsEntry("username", userExtension.getUsername())
          .containsEntry("organizationId", ORGANIZATION_ID);
    }

    @Test
    @DisplayName("should delegate to the account verification mapper")
    void shouldDelegateToAccountVerificationMapper() {
      notificationService.sendAccountVerification(ORGANIZATION_ID, userExtension);

      verify(notificationMapper).toAccountVerificationRequest(eq(userExtension), any(), any());
    }

    @Test
    @DisplayName("should publish the mapped notification event")
    void shouldPublishMappedNotificationEvent() {
      notificationService.sendAccountVerification(ORGANIZATION_ID, userExtension);

      verify(notificationEventPublisher).publish(mappedRequest);
    }
  }

  @Nested
  @DisplayName("sendEmailVerification")
  class SendEmailVerification {

    private static final String EMAIL_LINK = "https://app.ebikes.test/verify?token=" + PLAIN_TOKEN;

    private final NotificationRequestEvent mappedRequest =
        NotificationRequestFixtures.emailVerification();

    @Captor private ArgumentCaptor<Map<String, Serializable>> variablesCaptor;

    @BeforeEach
    void setUp() {
      when(tokenService.generateToken(TokenType.EMAIL_OTP, userExtension.getId()))
          .thenReturn(generatedToken);
      when(verificationLinkBuilder.buildEmailVerificationLink(PLAIN_TOKEN)).thenReturn(EMAIL_LINK);
      when(notificationMapper.toEmailVerificationRequest(eq(userExtension), any(), any()))
          .thenReturn(mappedRequest);
    }

    @Test
    @DisplayName("should generate an EMAIL_OTP token for the user")
    void shouldGenerateEmailOtpToken() {
      notificationService.sendEmailVerification(ORGANIZATION_ID, userExtension);

      verify(tokenService).generateToken(TokenType.EMAIL_OTP, userExtension.getId());
    }

    @Test
    @DisplayName("should build an email verification link from the plain token")
    void shouldBuildEmailVerificationLink() {
      notificationService.sendEmailVerification(ORGANIZATION_ID, userExtension);

      verify(verificationLinkBuilder).buildEmailVerificationLink(PLAIN_TOKEN);
    }

    @Test
    @DisplayName("should pass the correct variables to the notification mapper")
    void shouldPassCorrectVariablesToMapper() {
      notificationService.sendEmailVerification(ORGANIZATION_ID, userExtension);

      verify(notificationMapper)
          .toEmailVerificationRequest(eq(userExtension), variablesCaptor.capture(), any());

      assertThat(variablesCaptor.getValue())
          .containsEntry("verificationLink", EMAIL_LINK)
          .containsEntry("organizationId", ORGANIZATION_ID);
    }

    @Test
    @DisplayName("should delegate to the email verification mapper")
    void shouldDelegateToEmailVerificationMapper() {
      notificationService.sendEmailVerification(ORGANIZATION_ID, userExtension);

      verify(notificationMapper).toEmailVerificationRequest(eq(userExtension), any(), any());
    }

    @Test
    @DisplayName("should publish the mapped notification event")
    void shouldPublishMappedNotificationEvent() {
      notificationService.sendEmailVerification(ORGANIZATION_ID, userExtension);

      verify(notificationEventPublisher).publish(mappedRequest);
    }
  }

  @Nested
  @DisplayName("sendPasswordReset")
  class SendPasswordReset {

    private static final String RESET_LINK =
        "https://app.ebikes.test/reset-password?token=" + PLAIN_TOKEN;

    private final NotificationRequestEvent mappedRequest =
        NotificationRequestFixtures.passwordReset();

    @Captor private ArgumentCaptor<Map<String, Serializable>> variablesCaptor;

    @BeforeEach
    void setUp() {
      when(tokenService.generateToken(TokenType.PASSWORD_RESET, userExtension.getId()))
          .thenReturn(generatedToken);
      when(verificationLinkBuilder.buildPasswordResetLink(PLAIN_TOKEN)).thenReturn(RESET_LINK);
      when(notificationMapper.toPasswordResetRequest(eq(userExtension), any(), any()))
          .thenReturn(mappedRequest);
    }

    @Test
    @DisplayName("should generate a PASSWORD_RESET token for the user")
    void shouldGeneratePasswordResetToken() {
      notificationService.sendPasswordReset(ORGANIZATION_ID, userExtension);

      verify(tokenService).generateToken(TokenType.PASSWORD_RESET, userExtension.getId());
    }

    @Test
    @DisplayName("should build a password reset link from the plain token")
    void shouldBuildPasswordResetLink() {
      notificationService.sendPasswordReset(ORGANIZATION_ID, userExtension);

      verify(verificationLinkBuilder).buildPasswordResetLink(PLAIN_TOKEN);
    }

    @Test
    @DisplayName("should pass the correct variables to the notification mapper")
    void shouldPassCorrectVariablesToMapper() {
      notificationService.sendPasswordReset(ORGANIZATION_ID, userExtension);

      verify(notificationMapper)
          .toPasswordResetRequest(eq(userExtension), variablesCaptor.capture(), any());

      assertThat(variablesCaptor.getValue())
          .containsEntry("verificationLink", RESET_LINK)
          .containsEntry("organizationId", ORGANIZATION_ID);
    }

    @Test
    @DisplayName("should delegate to the password reset mapper")
    void shouldDelegateToPasswordResetMapper() {
      notificationService.sendPasswordReset(ORGANIZATION_ID, userExtension);

      verify(notificationMapper).toPasswordResetRequest(eq(userExtension), any(), any());
    }

    @Test
    @DisplayName("should publish the mapped notification event")
    void shouldPublishMappedNotificationEvent() {
      notificationService.sendPasswordReset(ORGANIZATION_ID, userExtension);

      verify(notificationEventPublisher).publish(mappedRequest);
    }
  }

  @Nested
  @DisplayName("sendPhoneVerification")
  class SendPhoneVerification {

    private final NotificationRequestEvent mappedRequest =
        NotificationRequestFixtures.phoneVerification();

    @Captor private ArgumentCaptor<Map<String, Serializable>> variablesCaptor;

    @BeforeEach
    void setUp() {
      when(tokenService.generateToken(TokenType.SMS_OTP, userExtension.getId()))
          .thenReturn(generatedToken);
      when(notificationMapper.toPhoneVerificationRequest(eq(userExtension), any(), any()))
          .thenReturn(mappedRequest);
    }

    @Test
    @DisplayName("should generate an SMS_OTP token for the user")
    void shouldGenerateSmsOtpToken() {
      notificationService.sendPhoneVerification(ORGANIZATION_ID, userExtension);

      verify(tokenService).generateToken(TokenType.SMS_OTP, userExtension.getId());
    }

    @Test
    @DisplayName("should pass the correct variables to the notification mapper")
    void shouldPassCorrectVariablesToMapper() {
      notificationService.sendPhoneVerification(ORGANIZATION_ID, userExtension);

      verify(notificationMapper)
          .toPhoneVerificationRequest(eq(userExtension), variablesCaptor.capture(), any());

      assertThat(variablesCaptor.getValue())
          .containsEntry("verificationCode", PLAIN_TOKEN)
          .containsEntry("verificationExpiry", EXPIRES_AT.toString())
          .containsEntry("username", userExtension.getUsername())
          .containsEntry("organizationId", ORGANIZATION_ID);
    }

    @Test
    @DisplayName("should delegate to the phone verification mapper")
    void shouldDelegateToPhoneVerificationMapper() {
      notificationService.sendPhoneVerification(ORGANIZATION_ID, userExtension);

      verify(notificationMapper).toPhoneVerificationRequest(eq(userExtension), any(), any());
    }

    @Test
    @DisplayName("should publish the mapped notification event")
    void shouldPublishMappedNotificationEvent() {
      notificationService.sendPhoneVerification(ORGANIZATION_ID, userExtension);

      verify(notificationEventPublisher).publish(mappedRequest);
    }
  }
}
