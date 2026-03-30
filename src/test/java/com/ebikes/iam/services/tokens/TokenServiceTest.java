package com.ebikes.iam.services.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.ebikes.iam.configurations.properties.TokenProperties;
import com.ebikes.iam.configurations.properties.TokenProperties.TokenConfiguration;
import com.ebikes.iam.database.entities.Token;
import com.ebikes.iam.database.repositories.TokenRepository;
import com.ebikes.iam.dtos.internal.GeneratedToken;
import com.ebikes.iam.enums.TokenType;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.mappers.TokenMapper;
import com.ebikes.iam.support.fixtures.TokenFixtures;

@DisplayName("TokenService")
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  @Mock private TokenMapper tokenMapper;
  @Mock private TokenProperties tokenProperties;
  @Mock private TokenRepository tokenRepository;

  @Mock private TokenConfiguration emailVerificationConfig;
  @Mock private TokenConfiguration emailOtpConfig;
  @Mock private TokenConfiguration passwordResetConfig;
  @Mock private TokenConfiguration smsOtpConfig;
  @Mock private TokenConfiguration twoFactorAuthConfig;
  @Mock private TokenConfiguration invalidConfig;

  @InjectMocks private TokenService tokenService;

  private final UUID userExtensionId = UUID.randomUUID();

  @Nested
  @DisplayName("consumeToken")
  class ConsumeToken {

    @Test
    @DisplayName("should consume and save active token")
    void shouldConsumeAndSaveActiveToken() {
      Token token = TokenFixtures.active(userExtensionId, TokenType.PASSWORD_RESET);

      when(tokenRepository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));

      tokenService.consumeToken(token.getTokenHash());

      assertThat(token.isConsumed()).isTrue();
      verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("should throw when token does not exist")
    void shouldThrowWhenTokenDoesNotExist() {
      when(tokenRepository.findByTokenHash("missing")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> tokenService.consumeToken("missing"))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw when token is already consumed")
    void shouldThrowWhenTokenIsAlreadyConsumed() {
      Token token = TokenFixtures.consumed(userExtensionId, TokenType.PASSWORD_RESET);
      String tokenHash = token.getTokenHash();

      when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

      assertThatThrownBy(() -> tokenService.consumeToken(tokenHash))
          .isInstanceOf(ValidationException.class);

      verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw when token is expired")
    void shouldThrowWhenTokenIsExpired() {
      Token token = TokenFixtures.expired(userExtensionId, TokenType.PASSWORD_RESET);
      String tokenHash = token.getTokenHash();

      when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

      assertThatThrownBy(() -> tokenService.consumeToken(tokenHash))
          .isInstanceOf(ValidationException.class);

      verify(tokenRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("generateToken")
  class GenerateToken {

    @Test
    @DisplayName("should generate and persist account activation token")
    void shouldGenerateAndPersistAccountActivationToken() {
      when(tokenProperties.getEmailVerification()).thenReturn(emailVerificationConfig);
      stubHoursConfig(emailVerificationConfig, 24);

      Token token = TokenFixtures.active(userExtensionId, TokenType.ACCOUNT_ACTIVATION);
      when(tokenMapper.toEntity(
              eq(userExtensionId), eq(TokenType.ACCOUNT_ACTIVATION), anyString(), any()))
          .thenReturn(token);

      GeneratedToken result =
          tokenService.generateToken(TokenType.ACCOUNT_ACTIVATION, userExtensionId);

      verify(tokenMapper)
          .toEntity(eq(userExtensionId), eq(TokenType.ACCOUNT_ACTIVATION), anyString(), any());
      verify(tokenRepository).save(token);
      assertThat(result).isNotNull();
      assertThat(result.token()).isEqualTo(token);
      assertThat(result.plainToken()).isNotBlank();
      assertThat(result.expiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusHours(23));
    }

    @Test
    @DisplayName("should generate and persist password reset token")
    void shouldGenerateAndPersistPasswordResetToken() {
      when(tokenProperties.getPasswordReset()).thenReturn(passwordResetConfig);
      stubHoursConfig(passwordResetConfig, 1);

      Token token = TokenFixtures.active(userExtensionId, TokenType.PASSWORD_RESET);
      when(tokenMapper.toEntity(
              eq(userExtensionId), eq(TokenType.PASSWORD_RESET), anyString(), any()))
          .thenReturn(token);

      GeneratedToken result = tokenService.generateToken(TokenType.PASSWORD_RESET, userExtensionId);

      verify(tokenRepository).save(token);
      assertThat(result.token()).isEqualTo(token);
      assertThat(result.plainToken()).isNotBlank();
    }

    @Test
    @DisplayName("should generate numeric otp for email otp")
    void shouldGenerateNumericOtpForEmailOtp() {
      when(tokenProperties.getEmailOtp()).thenReturn(emailOtpConfig);
      stubOtpLength(emailOtpConfig);

      Token token = TokenFixtures.active(userExtensionId, TokenType.EMAIL_OTP);
      when(tokenMapper.toEntity(eq(userExtensionId), eq(TokenType.EMAIL_OTP), anyString(), any()))
          .thenReturn(token);

      GeneratedToken result = tokenService.generateToken(TokenType.EMAIL_OTP, userExtensionId);

      verify(tokenRepository).save(token);
      assertThat(result.plainToken()).matches("\\d{6}");
    }

    @Test
    @DisplayName("should generate numeric otp for sms otp")
    void shouldGenerateNumericOtpForSmsOtp() {
      when(tokenProperties.getSmsOtp()).thenReturn(smsOtpConfig);
      stubOtpLength(smsOtpConfig);

      Token token = TokenFixtures.active(userExtensionId, TokenType.SMS_OTP);
      when(tokenMapper.toEntity(eq(userExtensionId), eq(TokenType.SMS_OTP), anyString(), any()))
          .thenReturn(token);

      GeneratedToken result = tokenService.generateToken(TokenType.SMS_OTP, userExtensionId);

      verify(tokenRepository).save(token);
      assertThat(result.plainToken()).matches("\\d{6}");
    }

    @Test
    @DisplayName("should generate numeric otp for two factor auth")
    void shouldGenerateNumericOtpForTwoFactorAuth() {
      when(tokenProperties.getTwoFactorAuth()).thenReturn(twoFactorAuthConfig);
      stubOtpLength(twoFactorAuthConfig);

      Token token = TokenFixtures.active(userExtensionId, TokenType.TWO_FACTOR_AUTH);
      when(tokenMapper.toEntity(
              eq(userExtensionId), eq(TokenType.TWO_FACTOR_AUTH), anyString(), any()))
          .thenReturn(token);

      GeneratedToken result =
          tokenService.generateToken(TokenType.TWO_FACTOR_AUTH, userExtensionId);

      verify(tokenRepository).save(token);
      assertThat(result.plainToken()).matches("\\d{6}");
    }

    @Test
    @DisplayName("should retry when token save fails with hash collision then succeed")
    void shouldRetryWhenTokenSaveFailsWithHashCollisionThenSucceed() {
      when(tokenProperties.getPasswordReset()).thenReturn(passwordResetConfig);
      stubHoursConfig(passwordResetConfig, 1);

      Token token = TokenFixtures.active(userExtensionId, TokenType.PASSWORD_RESET);
      when(tokenMapper.toEntity(
              eq(userExtensionId), eq(TokenType.PASSWORD_RESET), anyString(), any()))
          .thenReturn(token);
      when(tokenRepository.save(token))
          .thenThrow(new DataIntegrityViolationException("collision"))
          .thenReturn(token);

      GeneratedToken result = tokenService.generateToken(TokenType.PASSWORD_RESET, userExtensionId);

      verify(tokenRepository, times(2)).save(token);
      verify(tokenMapper, times(2))
          .toEntity(eq(userExtensionId), eq(TokenType.PASSWORD_RESET), anyString(), any());
      assertThat(result.token()).isEqualTo(token);
    }

    @Test
    @DisplayName("should throw when all token generation attempts collide")
    void shouldThrowWhenAllTokenGenerationAttemptsCollide() {
      when(tokenProperties.getPasswordReset()).thenReturn(passwordResetConfig);
      stubHoursConfig(passwordResetConfig, 1);

      Token token = TokenFixtures.active(userExtensionId, TokenType.PASSWORD_RESET);
      when(tokenMapper.toEntity(
              eq(userExtensionId), eq(TokenType.PASSWORD_RESET), anyString(), any()))
          .thenReturn(token);
      when(tokenRepository.save(token)).thenThrow(new DataIntegrityViolationException("collision"));

      assertThatThrownBy(
              () -> tokenService.generateToken(TokenType.PASSWORD_RESET, userExtensionId))
          .isInstanceOf(IllegalStateException.class);

      verify(tokenRepository, times(3)).save(token);
      verify(tokenMapper, times(3))
          .toEntity(eq(userExtensionId), eq(TokenType.PASSWORD_RESET), anyString(), any());
    }

    @Test
    @DisplayName("should calculate expiry using hours when configured")
    void shouldCalculateExpiryUsingHoursWhenConfigured() {
      when(tokenProperties.getPasswordReset()).thenReturn(passwordResetConfig);
      stubHoursConfig(passwordResetConfig, 1);

      Token token = TokenFixtures.active(userExtensionId, TokenType.PASSWORD_RESET);
      when(tokenMapper.toEntity(
              eq(userExtensionId), eq(TokenType.PASSWORD_RESET), anyString(), any()))
          .thenReturn(token);

      OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

      GeneratedToken result = tokenService.generateToken(TokenType.PASSWORD_RESET, userExtensionId);

      OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

      assertThat(result.expiresAt()).isAfterOrEqualTo(before.plusHours(1));
      assertThat(result.expiresAt()).isBeforeOrEqualTo(after.plusHours(1).plusSeconds(1));
    }

    @Test
    @DisplayName("should calculate expiry using minutes when configured")
    void shouldCalculateExpiryUsingMinutesWhenConfigured() {
      when(tokenProperties.getEmailOtp()).thenReturn(emailOtpConfig);
      stubOtpLength(emailOtpConfig);
      stubMinutesExpiry(emailOtpConfig);

      Token token = TokenFixtures.active(userExtensionId, TokenType.EMAIL_OTP);
      when(tokenMapper.toEntity(eq(userExtensionId), eq(TokenType.EMAIL_OTP), anyString(), any()))
          .thenReturn(token);

      OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

      GeneratedToken result = tokenService.generateToken(TokenType.EMAIL_OTP, userExtensionId);

      OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

      assertThat(result.expiresAt()).isAfterOrEqualTo(before.plusMinutes(10));
      assertThat(result.expiresAt()).isBeforeOrEqualTo(after.plusMinutes(10).plusSeconds(1));
    }

    @Test
    @DisplayName("should throw when token config has no validity duration")
    void shouldThrowWhenTokenConfigHasNoValidityDuration() {
      when(tokenProperties.getPasswordReset()).thenReturn(invalidConfig);
      when(invalidConfig.getValidityHours()).thenReturn(null);
      when(invalidConfig.getValidityMinutes()).thenReturn(null);

      assertThatThrownBy(
              () -> tokenService.generateToken(TokenType.PASSWORD_RESET, userExtensionId))
          .isInstanceOf(IllegalStateException.class);

      verify(tokenMapper, never()).toEntity(any(), any(), anyString(), any());
      verify(tokenRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getUserExtensionIdFromToken")
  class GetUserExtensionIdFromToken {

    @Test
    @DisplayName("should return user extension id when token exists")
    void shouldReturnUserExtensionIdWhenTokenExists() {
      Token token = TokenFixtures.active(userExtensionId, TokenType.PASSWORD_RESET);

      when(tokenRepository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));

      UUID result = tokenService.getUserExtensionIdFromToken(token.getTokenHash());

      assertThat(result).isEqualTo(userExtensionId);
    }

    @Test
    @DisplayName("should throw when token does not exist")
    void shouldThrowWhenTokenDoesNotExist() {
      when(tokenRepository.findByTokenHash("missing")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> tokenService.getUserExtensionIdFromToken("missing"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("invalidateUserTokens")
  class InvalidateUserTokens {

    @Test
    @DisplayName("should invalidate matching user tokens")
    void shouldInvalidateMatchingUserTokens() {
      when(tokenRepository.invalidateTokensByUserAndType(userExtensionId, TokenType.PASSWORD_RESET))
          .thenReturn(2);

      tokenService.invalidateUserTokens(TokenType.PASSWORD_RESET, userExtensionId);

      verify(tokenRepository)
          .invalidateTokensByUserAndType(userExtensionId, TokenType.PASSWORD_RESET);
    }

    @Test
    @DisplayName("should return quietly when no tokens were invalidated")
    void shouldReturnQuietlyWhenNoTokensWereInvalidated() {
      when(tokenRepository.invalidateTokensByUserAndType(userExtensionId, TokenType.PASSWORD_RESET))
          .thenReturn(0);

      tokenService.invalidateUserTokens(TokenType.PASSWORD_RESET, userExtensionId);

      verify(tokenRepository)
          .invalidateTokensByUserAndType(userExtensionId, TokenType.PASSWORD_RESET);
    }
  }

  @Nested
  @DisplayName("validateToken")
  class ValidateToken {

    @Test
    @DisplayName("should validate active token of expected type")
    void shouldValidateActiveTokenOfExpectedType() {
      Token token = TokenFixtures.active(userExtensionId, TokenType.PASSWORD_RESET);

      when(tokenRepository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));

      tokenService.validateToken(token.getTokenHash(), TokenType.PASSWORD_RESET);

      verify(tokenRepository).findByTokenHash(token.getTokenHash());
    }

    @Test
    @DisplayName("should throw when token does not exist")
    void shouldThrowWhenTokenDoesNotExist() {
      when(tokenRepository.findByTokenHash("missing")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> tokenService.validateToken("missing", TokenType.PASSWORD_RESET))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw when token type does not match")
    void shouldThrowWhenTokenTypeDoesNotMatch() {
      Token token = TokenFixtures.active(userExtensionId, TokenType.EMAIL_OTP);
      String tokenHash = token.getTokenHash();

      when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

      assertThatThrownBy(() -> tokenService.validateToken(tokenHash, TokenType.PASSWORD_RESET))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw when token is already consumed")
    void shouldThrowWhenTokenIsAlreadyConsumed() {
      Token token = TokenFixtures.consumed(userExtensionId, TokenType.PASSWORD_RESET);
      String tokenHash = token.getTokenHash();

      when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

      assertThatThrownBy(() -> tokenService.validateToken(tokenHash, TokenType.PASSWORD_RESET))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw when token is expired")
    void shouldThrowWhenTokenIsExpired() {
      Token token = TokenFixtures.expired(userExtensionId, TokenType.PASSWORD_RESET);
      String tokenHash = token.getTokenHash();

      when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

      assertThatThrownBy(() -> tokenService.validateToken(tokenHash, TokenType.PASSWORD_RESET))
          .isInstanceOf(ValidationException.class);
    }
  }

  private void stubHoursConfig(TokenConfiguration config, int validityHours) {
    when(config.getLength()).thenReturn(32);
    when(config.getValidityHours()).thenReturn(validityHours);
  }

  private void stubOtpLength(TokenConfiguration config) {
    when(config.getLength()).thenReturn(6);
  }

  private void stubMinutesExpiry(TokenConfiguration config) {
    when(config.getValidityHours()).thenReturn(null);
    when(config.getValidityMinutes()).thenReturn(10);
  }
}
