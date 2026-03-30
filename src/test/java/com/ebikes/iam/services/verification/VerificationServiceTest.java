package com.ebikes.iam.services.verification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.TokenType;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.services.notifications.NotificationService;
import com.ebikes.iam.services.ratelimit.RateLimitService;
import com.ebikes.iam.services.tokens.TokenService;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.audit.ThrowingRunnable;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;
import com.ebikes.iam.support.security.TokenHashUtilities;

@DisplayName("VerificationService")
@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

  private static final String PLAIN_TOKEN = "plain-token-abc123";
  private static final String TOKEN_HASH = TokenHashUtilities.hashToken(PLAIN_TOKEN);

  @Mock private AuditTemplate auditTemplate;
  @Mock private KeycloakUserAdapter keycloakUserAdapter;
  @Mock private NotificationService notificationService;
  @Mock private RateLimitService rateLimitService;
  @Mock private TokenService tokenService;
  @Mock private UserExtensionService userExtensionService;

  @InjectMocks private VerificationService verificationService;

  private UserExtension activeUser;

  @BeforeEach
  void setUp() {
    activeUser = UserExtensionFixtures.persisted();
  }

  @SuppressWarnings("unchecked")
  private void stubAuditTemplateToExecute() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> operation = invocation.getArgument(1);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(AuditContext.class), any(ThrowingRunnable.class));
  }

  @Nested
  @DisplayName("completeAccountActivation")
  class CompleteAccountActivation {

    @BeforeEach
    void setUp() {
      stubAuditTemplateToExecute();
      when(tokenService.getUserExtensionIdFromToken(TOKEN_HASH)).thenReturn(activeUser.getId());
      when(userExtensionService.findUserExtensionById(activeUser.getId())).thenReturn(activeUser);
    }

    @Test
    @DisplayName("should validate the token as ACCOUNT_ACTIVATION type")
    void shouldValidateTokenAsAccountActivationType() {
      verificationService.completeAccountActivation("newPassword", PLAIN_TOKEN);

      verify(tokenService).validateToken(TOKEN_HASH, TokenType.ACCOUNT_ACTIVATION);
    }

    @Test
    @DisplayName("should reset the password in Keycloak")
    void shouldResetPasswordInKeycloak() {
      verificationService.completeAccountActivation("newPassword", PLAIN_TOKEN);

      verify(keycloakUserAdapter)
          .resetPassword(activeUser.getKeycloakUserId(), "newPassword", false);
    }

    @Test
    @DisplayName("should verify the email in Keycloak")
    void shouldVerifyEmailInKeycloak() {
      verificationService.completeAccountActivation("newPassword", PLAIN_TOKEN);

      verify(keycloakUserAdapter).verifyEmail(activeUser.getKeycloakUserId());
    }

    @Test
    @DisplayName("should enable the user in Keycloak")
    void shouldEnableUserInKeycloak() {
      verificationService.completeAccountActivation("newPassword", PLAIN_TOKEN);

      verify(keycloakUserAdapter).enableUser(activeUser.getKeycloakUserId());
    }

    @Test
    @DisplayName("should activate the user extension")
    void shouldActivateUserExtension() {
      verificationService.completeAccountActivation("newPassword", PLAIN_TOKEN);

      verify(userExtensionService).activate(activeUser);
    }

    @Test
    @DisplayName("should consume the token")
    void shouldConsumeToken() {
      verificationService.completeAccountActivation("newPassword", PLAIN_TOKEN);

      verify(tokenService).consumeToken(TOKEN_HASH);
    }
  }

  @Nested
  @DisplayName("completeEmailVerification")
  class CompleteEmailVerification {

    @BeforeEach
    void setUp() {
      stubAuditTemplateToExecute();
      when(tokenService.getUserExtensionIdFromToken(TOKEN_HASH)).thenReturn(activeUser.getId());
      when(userExtensionService.findUserExtensionById(activeUser.getId())).thenReturn(activeUser);
    }

    @Test
    @DisplayName("should validate the token as EMAIL_OTP type")
    void shouldValidateTokenAsEmailOtpType() {
      verificationService.completeEmailVerification(PLAIN_TOKEN);

      verify(tokenService).validateToken(TOKEN_HASH, TokenType.EMAIL_OTP);
    }

    @Test
    @DisplayName("should verify the email on the user extension")
    void shouldVerifyEmailOnUserExtension() {
      verificationService.completeEmailVerification(PLAIN_TOKEN);

      verify(userExtensionService).verifyEmail(activeUser);
    }

    @Test
    @DisplayName("should consume the token")
    void shouldConsumeToken() {
      verificationService.completeEmailVerification(PLAIN_TOKEN);

      verify(tokenService).consumeToken(TOKEN_HASH);
    }
  }

  @Nested
  @DisplayName("completePasswordReset")
  class CompletePasswordReset {

    @BeforeEach
    void setUp() {
      stubAuditTemplateToExecute();
      when(tokenService.getUserExtensionIdFromToken(TOKEN_HASH)).thenReturn(activeUser.getId());
      when(userExtensionService.findUserExtensionById(activeUser.getId())).thenReturn(activeUser);
    }

    @Test
    @DisplayName("should validate the token as PASSWORD_RESET type")
    void shouldValidateTokenAsPasswordResetType() {
      verificationService.completePasswordReset("newPassword", PLAIN_TOKEN);

      verify(tokenService).validateToken(TOKEN_HASH, TokenType.PASSWORD_RESET);
    }

    @Test
    @DisplayName("should reset the password in Keycloak")
    void shouldResetPasswordInKeycloak() {
      verificationService.completePasswordReset("newPassword", PLAIN_TOKEN);

      verify(keycloakUserAdapter)
          .resetPassword(activeUser.getKeycloakUserId(), "newPassword", false);
    }

    @Test
    @DisplayName("should consume the token")
    void shouldConsumeToken() {
      verificationService.completePasswordReset("newPassword", PLAIN_TOKEN);

      verify(tokenService).consumeToken(TOKEN_HASH);
    }
  }

  @Nested
  @DisplayName("completePhoneNumberVerification")
  class CompletePhoneNumberVerification {

    @BeforeEach
    void setUp() {
      stubAuditTemplateToExecute();
      when(tokenService.getUserExtensionIdFromToken(TOKEN_HASH)).thenReturn(activeUser.getId());
      when(userExtensionService.findUserExtensionById(activeUser.getId())).thenReturn(activeUser);
    }

    @Test
    @DisplayName("should validate the token as SMS_OTP type")
    void shouldValidateTokenAsSmsOtpType() {
      verificationService.completePhoneNumberVerification(PLAIN_TOKEN);

      verify(tokenService).validateToken(TOKEN_HASH, TokenType.SMS_OTP);
    }

    @Test
    @DisplayName("should verify the phone in Keycloak")
    void shouldVerifyPhoneInKeycloak() {
      verificationService.completePhoneNumberVerification(PLAIN_TOKEN);

      verify(keycloakUserAdapter).verifyPhone(activeUser.getKeycloakUserId());
    }

    @Test
    @DisplayName("should verify the phone on the user extension")
    void shouldVerifyPhoneOnUserExtension() {
      verificationService.completePhoneNumberVerification(PLAIN_TOKEN);

      verify(userExtensionService).verifyPhone(activeUser);
    }

    @Test
    @DisplayName("should consume the token")
    void shouldConsumeToken() {
      verificationService.completePhoneNumberVerification(PLAIN_TOKEN);

      verify(tokenService).consumeToken(TOKEN_HASH);
    }
  }

  @Nested
  @DisplayName("requestEmailVerification")
  class RequestEmailVerification {

    private static final String EMAIL = "user@ebikes.test";

    @Test
    @DisplayName("should do nothing when no user exists for the email")
    void shouldDoNothingWhenUserNotFound() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL)).thenReturn(Optional.empty());

      verificationService.requestEmailVerification(EMAIL);

      verify(notificationService, never()).sendEmailVerification(any(), any());
    }

    @Test
    @DisplayName("should reject an inactive user")
    void shouldRejectInactiveUser() {
      UserExtension inactiveUser = UserExtensionFixtures.inactive();
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(inactiveUser));

      assertThatThrownBy(() -> verificationService.requestEmailVerification(EMAIL))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should check the resend rate limit")
    void shouldCheckResendRateLimit() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestEmailVerification(EMAIL);

      verify(rateLimitService).checkResendLimit(any(), eq("EMAIL_VERIFICATION"), any());
    }

    @Test
    @DisplayName("should invalidate existing EMAIL_OTP tokens")
    void shouldInvalidateExistingEmailOtpTokens() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestEmailVerification(EMAIL);

      verify(tokenService).invalidateUserTokens(TokenType.EMAIL_OTP, activeUser.getId());
    }

    @Test
    @DisplayName("should dispatch an email verification notification")
    void shouldDispatchEmailVerificationNotification() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestEmailVerification(EMAIL);

      verify(notificationService).sendEmailVerification(activeUser.getOrganizationId(), activeUser);
    }
  }

  @Nested
  @DisplayName("requestPasswordReset")
  class RequestPasswordReset {

    private static final String EMAIL = "user@ebikes.test";

    @Test
    @DisplayName("should do nothing when no user exists for the email")
    void shouldDoNothingWhenUserNotFound() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL)).thenReturn(Optional.empty());

      verificationService.requestPasswordReset(EMAIL);

      verify(notificationService, never()).sendPasswordReset(any(), any());
    }

    @Test
    @DisplayName("should not reject an inactive user")
    void shouldNotRejectInactiveUser() {
      UserExtension inactiveUser = UserExtensionFixtures.persisted();
      inactiveUser.updateStatus(UserStatus.INACTIVE);
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(inactiveUser));

      verificationService.requestPasswordReset(EMAIL);

      verify(notificationService).sendPasswordReset(inactiveUser.getOrganizationId(), inactiveUser);
    }

    @Test
    @DisplayName("should check the resend rate limit")
    void shouldCheckResendRateLimit() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestPasswordReset(EMAIL);

      verify(rateLimitService).checkResendLimit(any(), eq("PASSWORD_RESET"), any());
    }

    @Test
    @DisplayName("should invalidate existing PASSWORD_RESET tokens")
    void shouldInvalidateExistingPasswordResetTokens() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestPasswordReset(EMAIL);

      verify(tokenService).invalidateUserTokens(TokenType.PASSWORD_RESET, activeUser.getId());
    }

    @Test
    @DisplayName("should dispatch a password reset notification")
    void shouldDispatchPasswordResetNotification() {
      when(userExtensionService.findUserExtensionByEmail(EMAIL))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestPasswordReset(EMAIL);

      verify(notificationService).sendPasswordReset(activeUser.getOrganizationId(), activeUser);
    }
  }

  @Nested
  @DisplayName("requestPhoneVerification")
  class RequestPhoneVerification {

    private static final String PHONE_NUMBER = "+254700000001";

    @Test
    @DisplayName("should do nothing when no user exists for the phone number")
    void shouldDoNothingWhenUserNotFound() {
      when(userExtensionService.findUserExtensionByPhoneNumber(PHONE_NUMBER))
          .thenReturn(Optional.empty());

      verificationService.requestPhoneVerification(PHONE_NUMBER);

      verify(notificationService, never()).sendPhoneVerification(any(), any());
    }

    @Test
    @DisplayName("should reject an inactive user")
    void shouldRejectInactiveUser() {
      UserExtension inactiveUser = UserExtensionFixtures.inactive();
      when(userExtensionService.findUserExtensionByPhoneNumber(PHONE_NUMBER))
          .thenReturn(Optional.of(inactiveUser));

      assertThatThrownBy(() -> verificationService.requestPhoneVerification(PHONE_NUMBER))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should check the resend rate limit")
    void shouldCheckResendRateLimit() {
      when(userExtensionService.findUserExtensionByPhoneNumber(PHONE_NUMBER))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestPhoneVerification(PHONE_NUMBER);

      verify(rateLimitService).checkResendLimit(any(), eq("PHONE_VERIFICATION"), any());
    }

    @Test
    @DisplayName("should invalidate existing SMS_OTP tokens")
    void shouldInvalidateExistingSmsOtpTokens() {
      when(userExtensionService.findUserExtensionByPhoneNumber(PHONE_NUMBER))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestPhoneVerification(PHONE_NUMBER);

      verify(tokenService).invalidateUserTokens(TokenType.SMS_OTP, activeUser.getId());
    }

    @Test
    @DisplayName("should dispatch a phone verification notification")
    void shouldDispatchPhoneVerificationNotification() {
      when(userExtensionService.findUserExtensionByPhoneNumber(PHONE_NUMBER))
          .thenReturn(Optional.of(activeUser));

      verificationService.requestPhoneVerification(PHONE_NUMBER);

      verify(notificationService).sendPhoneVerification(activeUser.getOrganizationId(), activeUser);
    }
  }
}
