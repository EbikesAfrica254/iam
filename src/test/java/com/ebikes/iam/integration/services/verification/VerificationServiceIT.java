package com.ebikes.iam.integration.services.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.database.entities.Token;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.OutboxRepository;
import com.ebikes.iam.database.repositories.TokenRepository;
import com.ebikes.iam.database.repositories.UserExtensionRepository;
import com.ebikes.iam.enums.TokenType;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.services.ratelimit.RateLimitService;
import com.ebikes.iam.services.verification.VerificationService;
import com.ebikes.iam.support.fixtures.TokenFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;
import com.ebikes.iam.support.infrastructure.AbstractIntegrationTest;
import com.ebikes.iam.support.security.TokenHashUtilities;

@TestPropertySource(properties = "notifications.enabled=true")
class VerificationServiceIT extends AbstractIntegrationTest {

  @Autowired private VerificationService verificationService;
  @Autowired private TokenRepository tokenRepository;
  @Autowired private UserExtensionRepository userExtensionRepository;
  @Autowired private OutboxRepository outboxRepository;

  @MockitoBean private KeycloakUserAdapter keycloakUserAdapter;
  @MockitoBean private RateLimitService rateLimitService;

  @BeforeEach
  void setUp() {
    outboxRepository.deleteAll();
    tokenRepository.deleteAll();
    userExtensionRepository.deleteAll();
  }

  @Nested
  @DisplayName("completeEmailVerification")
  class CompleteEmailVerification {

    @Test
    @DisplayName("completes email verification and consumes token when token is valid")
    void shouldVerifyEmailAndConsumeTokenWhenTokenIsValid() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.inactive());
      PersistedToken token = persistToken(user.getId(), TokenType.EMAIL_OTP, false, false);
      String testToken = token.rawToken();
      verificationService.completeEmailVerification(testToken);

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isTrue();
      assertThat(findUser(user.getId()).isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("rejects email verification when token is expired")
    void shouldRejectExpiredToken() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.inactive());
      PersistedToken token = persistToken(user.getId(), TokenType.EMAIL_OTP, true, false);
      String testToken = token.rawToken();
      assertThatThrownBy(() -> verificationService.completeEmailVerification(testToken))
          .isInstanceOf(ValidationException.class);

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isFalse();
      assertThat(findUser(user.getId()).isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("rejects email verification when token is already consumed")
    void shouldRejectAlreadyConsumedToken() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.inactive());
      PersistedToken token = persistToken(user.getId(), TokenType.EMAIL_OTP, false, true);
      String testToken = token.rawToken();
      assertThatThrownBy(() -> verificationService.completeEmailVerification(testToken))
          .isInstanceOf(ValidationException.class);

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isTrue();
      assertThat(findUser(user.getId()).isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("rejects email verification when token type does not match")
    void shouldRejectTokenTypeMismatch() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.inactive());
      PersistedToken token = persistToken(user.getId(), TokenType.PASSWORD_RESET, false, false);

      String testToken = token.rawToken();
      assertThatThrownBy(() -> verificationService.completeEmailVerification(testToken))
          .isInstanceOf(ValidationException.class);

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isFalse();
      assertThat(findUser(user.getId()).isEmailVerified()).isFalse();
    }
  }

  @Nested
  @DisplayName("completePasswordReset")
  class CompletePasswordReset {

    @Test
    @DisplayName("completes password reset, consumes token, and calls Keycloak when token is valid")
    void shouldConsumeTokenAndCallKeycloakWhenTokenIsValid() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.active());
      PersistedToken token = persistToken(user.getId(), TokenType.PASSWORD_RESET, false, false);

      verificationService.completePasswordReset("NewPassword123!", token.rawToken());

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isTrue();
      verify(keycloakUserAdapter).resetPassword(user.getKeycloakUserId(), "NewPassword123!", false);
    }

    @Test
    @DisplayName("rolls back token consumption when Keycloak password reset fails")
    void shouldRollbackTokenConsumptionWhenKeycloakResetFails() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.active());
      PersistedToken token = persistToken(user.getId(), TokenType.PASSWORD_RESET, false, false);

      doThrow(new RuntimeException("Keycloak failure"))
          .when(keycloakUserAdapter)
          .resetPassword(anyString(), anyString(), anyBoolean());

      String testToken = token.rawToken();
      assertThatThrownBy(
              () -> verificationService.completePasswordReset("NewPassword123!", testToken))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Keycloak failure");

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isFalse();
    }
  }

  @Nested
  @DisplayName("completeAccountActivation")
  class CompleteAccountActivation {

    @Test
    @DisplayName(
        "completes account activation, consumes token, and calls Keycloak when token is valid")
    void shouldActivateUserConsumeTokenAndCallKeycloakWhenTokenIsValid() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.inactive());
      PersistedToken token = persistToken(user.getId(), TokenType.ACCOUNT_ACTIVATION, false, false);

      verificationService.completeAccountActivation("StrongPassword123!", token.rawToken());

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isTrue();
      assertThat(findUser(user.getId()).getStatus()).isEqualTo(UserStatus.ACTIVE);
      verify(keycloakUserAdapter)
          .resetPassword(user.getKeycloakUserId(), "StrongPassword123!", false);
      verify(keycloakUserAdapter).verifyEmail(user.getKeycloakUserId());
      verify(keycloakUserAdapter).enableUser(user.getKeycloakUserId());
    }

    @Test
    @DisplayName("rolls back local state when Keycloak operation fails")
    void shouldRollbackLocalStateWhenKeycloakOperationFails() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.inactive());
      PersistedToken token = persistToken(user.getId(), TokenType.ACCOUNT_ACTIVATION, false, false);

      doThrow(new RuntimeException("Keycloak failure"))
          .when(keycloakUserAdapter)
          .verifyEmail(anyString());

      String testToken = token.rawToken();
      assertThatThrownBy(
              () -> verificationService.completeAccountActivation("StrongPassword123!", testToken))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Keycloak failure");

      assertThat(findTokenByHash(token.tokenHash()).isConsumed()).isFalse();
      assertThat(findUser(user.getId()).getStatus()).isEqualTo(UserStatus.INACTIVE);
    }
  }

  @Nested
  @DisplayName("requestEmailVerification")
  class RequestEmailVerification {

    @Test
    @DisplayName(
        "invalidates existing email verification token, creates a new token, and writes an outbox"
            + " event")
    void shouldInvalidateExistingTokenCreateNewTokenAndWriteOutbox() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.active());
      Token oldToken =
          tokenRepository.saveAndFlush(TokenFixtures.active(user.getId(), TokenType.EMAIL_OTP));

      long outboxCountBefore = outboxRepository.count();

      verificationService.requestEmailVerification(user.getEmail());

      List<Token> userTokens = findTokensByType(user.getId(), TokenType.EMAIL_OTP);
      List<Token> activeTokens = findActiveTokensByType(user.getId(), TokenType.EMAIL_OTP);

      assertThat(userTokens).hasSize(2);
      assertThat(activeTokens).hasSize(1);
      assertThat(activeTokens.getFirst().getTokenHash()).isNotEqualTo(oldToken.getTokenHash());
      assertThat(outboxRepository.count()).isEqualTo(outboxCountBefore + 1);
    }

    @Test
    @DisplayName("does nothing when email verification is requested for unknown email")
    void shouldDoNothingForUnknownEmail() {
      verificationService.requestEmailVerification("unknown@ebikes.test");

      assertThat(tokenRepository.findAll()).isEmpty();
      assertThat(outboxRepository.findAll()).isEmpty();
    }
  }

  @Nested
  @DisplayName("requestPasswordReset")
  class RequestPasswordReset {

    @Test
    @DisplayName(
        "invalidates existing password reset token, creates a new token, and writes an outbox"
            + " event")
    void shouldInvalidateExistingTokenCreateNewTokenAndWriteOutbox() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.active());
      Token oldToken =
          tokenRepository.saveAndFlush(
              TokenFixtures.active(user.getId(), TokenType.PASSWORD_RESET));

      long outboxCountBefore = outboxRepository.count();

      verificationService.requestPasswordReset(user.getEmail());

      List<Token> userTokens = findTokensByType(user.getId(), TokenType.PASSWORD_RESET);
      List<Token> activeTokens = findActiveTokensByType(user.getId(), TokenType.PASSWORD_RESET);

      assertThat(userTokens).hasSize(2);
      assertThat(activeTokens).hasSize(1);
      assertThat(activeTokens.getFirst().getTokenHash()).isNotEqualTo(oldToken.getTokenHash());
      assertThat(outboxRepository.count()).isEqualTo(outboxCountBefore + 1);
    }
  }

  @Nested
  @DisplayName("requestPhoneVerification")
  class RequestPhoneVerification {

    @Test
    @DisplayName("rejects phone verification request for an inactive user")
    void shouldRejectInactiveUser() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.inactive());

      String phoneNumber = user.getPhoneNumber();
      assertThatThrownBy(() -> verificationService.requestPhoneVerification(phoneNumber))
          .isInstanceOf(ValidationException.class);

      assertThat(tokenRepository.findAll()).isEmpty();
      assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName(
        "invalidates existing phone verification token, creates a new token, and writes an outbox"
            + " event")
    void shouldInvalidateExistingTokenCreateNewTokenAndWriteOutbox() {
      UserExtension user = userExtensionRepository.saveAndFlush(UserExtensionFixtures.active());
      Token oldToken =
          tokenRepository.saveAndFlush(TokenFixtures.active(user.getId(), TokenType.SMS_OTP));

      long outboxCountBefore = outboxRepository.count();

      verificationService.requestPhoneVerification(user.getPhoneNumber());

      List<Token> userTokens = findTokensByType(user.getId(), TokenType.SMS_OTP);
      List<Token> activeTokens = findActiveTokensByType(user.getId(), TokenType.SMS_OTP);

      assertThat(userTokens).hasSize(2);
      assertThat(activeTokens).hasSize(1);
      assertThat(activeTokens.getFirst().getTokenHash()).isNotEqualTo(oldToken.getTokenHash());
      assertThat(outboxRepository.count()).isEqualTo(outboxCountBefore + 1);
    }
  }

  private UserExtension findUser(UUID userId) {
    return userExtensionRepository.findById(userId).orElseThrow();
  }

  private Token findTokenByHash(String tokenHash) {
    return tokenRepository.findAll().stream()
        .filter(token -> token.getTokenHash().equals(tokenHash))
        .findFirst()
        .orElseThrow();
  }

  private List<Token> findTokensByType(UUID userExtensionId, TokenType tokenType) {
    return tokenRepository.findAll().stream()
        .filter(token -> token.getUserExtensionId().equals(userExtensionId))
        .filter(token -> token.getTokenType() == tokenType)
        .toList();
  }

  private List<Token> findActiveTokensByType(UUID userExtensionId, TokenType tokenType) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    return findTokensByType(userExtensionId, tokenType).stream()
        .filter(token -> !token.isConsumed())
        .filter(token -> token.getExpiresAt().isAfter(now))
        .toList();
  }

  private PersistedToken persistToken(
      UUID userExtensionId, TokenType tokenType, boolean expired, boolean consumed) {
    String rawToken = UUID.randomUUID().toString();
    String tokenHash = TokenHashUtilities.hashToken(rawToken);

    Token token =
        Token.builder()
            .userExtensionId(userExtensionId)
            .tokenType(tokenType)
            .tokenHash(tokenHash)
            .consumed(false)
            .expiresAt(
                expired
                    ? OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)
                    : OffsetDateTime.now(ZoneOffset.UTC).plusHours(24))
            .version(0L)
            .build();

    if (consumed) {
      token.consume();
    }

    tokenRepository.saveAndFlush(token);
    return new PersistedToken(rawToken, tokenHash);
  }

  private record PersistedToken(String rawToken, String tokenHash) {}
}
