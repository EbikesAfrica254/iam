package com.ebikes.iam.services.tokens;

import com.ebikes.iam.configurations.properties.TokenProperties;
import com.ebikes.iam.configurations.properties.TokenProperties.TokenConfiguration;
import com.ebikes.iam.database.entities.Token;
import com.ebikes.iam.database.repositories.TokenRepository;
import com.ebikes.iam.dtos.internal.GeneratedToken;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.TokenType;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.mappers.TokenMapper;
import com.ebikes.iam.support.security.TokenHashUtilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class TokenService {

    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TokenMapper tokenMapper;
    private final TokenProperties tokenProperties;
    private final TokenRepository tokenRepository;

    @Transactional
    public void consumeToken(String tokenHash) {
        log.debug("Consuming token");

        Token token = findByHashOrThrow(tokenHash);
        checkTokenState(token);

        token.consume();
        tokenRepository.save(token);

        log.info(
                "Token consumed - tokenType={} userExtensionId={}",
                token.getTokenType(),
                token.getUserExtensionId());
    }

    @Transactional
    public GeneratedToken generateToken(TokenType tokenType, UUID userExtensionId) {
        log.info("Generating token - tokenType={} userExtensionId={}", tokenType, userExtensionId);

        TokenConfiguration config = getTokenConfiguration(tokenType);
        OffsetDateTime expiresAt = calculateExpiryTime(OffsetDateTime.now(ZoneOffset.UTC), config);

        for (int attempts = 1; attempts <= MAX_TOKEN_GENERATION_ATTEMPTS; attempts++) {
            String plainToken = generatePlainToken(tokenType, config.getLength());
            String tokenHash = TokenHashUtilities.hashToken(plainToken);

            try {
                Token token = tokenMapper.toEntity(userExtensionId, tokenType, tokenHash, expiresAt);
                tokenRepository.save(token);

                log.info("Token generated - tokenType={} userExtensionId={}", tokenType, userExtensionId);
                return new GeneratedToken(expiresAt, plainToken, token);

            } catch (DataIntegrityViolationException e) {
                log.warn(
                        "Token hash collision on attempt {}/{} - tokenType={} userExtensionId={}",
                        attempts,
                        MAX_TOKEN_GENERATION_ATTEMPTS,
                        tokenType,
                        userExtensionId);
            }
        }

        throw new IllegalStateException(
                "Failed to generate unique token after "
                        + MAX_TOKEN_GENERATION_ATTEMPTS
                        + " attempts"
                        + " - tokenType="
                        + tokenType
                        + " userExtensionId="
                        + userExtensionId);
    }

    @Transactional(readOnly = true)
    public UUID getUserExtensionIdFromToken(String tokenHash) {
        log.debug("Retrieving userExtensionId from token");
        return findByHashOrThrow(tokenHash).getUserExtensionId();
    }

    @Transactional
    public void invalidateUserTokens(TokenType tokenType, UUID userExtensionId) {
        log.info("Invalidating tokens - tokenType={} userExtensionId={}", tokenType, userExtensionId);

        int invalidatedCount =
                tokenRepository.invalidateTokensByUserAndType(userExtensionId, tokenType);

        if (invalidatedCount == 0) {
            log.warn(
                    "No tokens found to invalidate - tokenType={} userExtensionId={}",
                    tokenType,
                    userExtensionId);
            return;
        }

        log.info(
                "Tokens invalidated - count={} tokenType={} userExtensionId={}",
                invalidatedCount,
                tokenType,
                userExtensionId);
    }

    @Transactional
    public void validateToken(String tokenHash, TokenType tokenType) {
        log.debug("Validating token - expectedType={}", tokenType);

        Token token = findByHashOrThrow(tokenHash);

        if (token.getTokenType() != tokenType) {
            log.warn(
                    "Token type mismatch - expected={} actual={} userExtensionId={}",
                    tokenType,
                    token.getTokenType(),
                    token.getUserExtensionId());
            throw new ValidationException(
                    ResponseCode.SECURITY_CODE_TYPE_MISMATCH,
                    "Expected code type " + tokenType + " but found " + token.getTokenType(),
                    "tokenType",
                    token.getTokenType().name());
        }

        checkTokenState(token);

        log.info(
                "Token validated - tokenType={} userExtensionId={}",
                token.getTokenType(),
                token.getUserExtensionId());
    }

    private OffsetDateTime calculateExpiryTime(OffsetDateTime now, TokenConfiguration config) {
        if (config.getValidityHours() != null) {
            return now.plusHours(config.getValidityHours());
        }
        if (config.getValidityMinutes() != null) {
            return now.plusMinutes(config.getValidityMinutes());
        }
        throw new IllegalStateException("Token config must define validity duration");
    }

    private void checkTokenState(Token token) {
        if (token.isConsumed()) {
            log.warn(
                    "Token already consumed - tokenType={} userExtensionId={}",
                    token.getTokenType(),
                    token.getUserExtensionId());
            throw new ValidationException(
                    ResponseCode.SECURITY_CODE_ALREADY_USED,
                    "Token has already been used and cannot be reused",
                    "tokenHash",
                    token.getTokenHash());
        }

        if (token.isExpired(OffsetDateTime.now(ZoneOffset.UTC))) {
            log.warn(
                    "Token expired - tokenType={} userExtensionId={}",
                    token.getTokenType(),
                    token.getUserExtensionId());
            throw new ValidationException(
                    ResponseCode.SECURITY_CODE_EXPIRED,
                    "Token has exceeded its validity period",
                    "tokenHash",
                    token.getTokenHash());
        }
    }

    private Token findByHashOrThrow(String tokenHash) {
        return tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        ResponseCode.RESOURCE_NOT_FOUND, "Token does not exist"));
    }

    private String generateNumericOtp(int length) {
        char[] digits = new char[length];
        for (int i = 0; i < length; i++) {
            digits[i] = (char) ('0' + SECURE_RANDOM.nextInt(10));
        }
        return new String(digits);
    }

    private String generatePlainToken(TokenType tokenType, int length) {
        return switch (tokenType) {
            case ACCOUNT_ACTIVATION, PASSWORD_RESET -> generateRandomToken(length);
            case EMAIL_OTP, SMS_OTP, TWO_FACTOR_AUTH -> generateNumericOtp(length);
        };
    }

    private String generateRandomToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private TokenConfiguration getTokenConfiguration(TokenType tokenType) {
        return switch (tokenType) {
            case ACCOUNT_ACTIVATION -> tokenProperties.getEmailVerification();
            case EMAIL_OTP -> tokenProperties.getEmailOtp();
            case PASSWORD_RESET -> tokenProperties.getPasswordReset();
            case SMS_OTP -> tokenProperties.getSmsOtp();
            case TWO_FACTOR_AUTH -> tokenProperties.getTwoFactorAuth();
        };
    }
}
