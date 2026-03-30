package com.ebikes.iam.services.ratelimit;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.ebikes.iam.configurations.properties.RateLimitProperties;
import com.ebikes.iam.configurations.properties.RateLimitProperties.ResendLimits;
import com.ebikes.iam.exceptions.RateLimitException;

@DisplayName("RateLimitService")
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

  private static final String IP_ADDRESS = "192.168.1.1";
  private static final String OPERATION = "email-verification";
  private static final String USER_EXTENSION_ID = "user-123";

  private static final String IP_KEY = "ratelimit:resend:ip:" + IP_ADDRESS + ":" + OPERATION;
  private static final String USER_KEY =
      "ratelimit:resend:user:" + USER_EXTENSION_ID + ":" + OPERATION;

  private static final int MAX_ATTEMPTS_PER_IP = 10;
  private static final int MAX_ATTEMPTS_PER_USER = 5;
  private static final int WINDOW_MINUTES = 60;

  @Mock private RateLimitProperties rateLimitProperties;
  @Mock private RedisTemplate<String, String> redisTemplate;

  private RateLimitService service;

  @BeforeEach
  void setUp() {
    ResendLimits limits = new ResendLimits();
    limits.setMaxAttemptsPerIp(MAX_ATTEMPTS_PER_IP);
    limits.setMaxAttemptsPerUser(MAX_ATTEMPTS_PER_USER);
    limits.setWindowMinutes(WINDOW_MINUTES);
    when(rateLimitProperties.getResend()).thenReturn(limits);

    service = new RateLimitService(rateLimitProperties, redisTemplate);
  }

  @Test
  @DisplayName("should pass when attempts are within IP and user limits")
  @SuppressWarnings("unchecked")
  void shouldPassWhenWithinLimits() {
    when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

    assertThatNoException()
        .isThrownBy(() -> service.checkResendLimit(IP_ADDRESS, OPERATION, USER_EXTENSION_ID));
  }

  @Test
  @DisplayName("should throw RateLimitException when IP limit is exceeded")
  @SuppressWarnings("unchecked")
  void shouldThrowWhenIpLimitExceeded() {
    when(redisTemplate.execute(
            any(RedisScript.class), argThat(keys -> keys.contains(IP_KEY)), anyString()))
        .thenReturn((long) MAX_ATTEMPTS_PER_IP + 1);

    assertThatThrownBy(() -> service.checkResendLimit(IP_ADDRESS, OPERATION, USER_EXTENSION_ID))
        .isInstanceOf(RateLimitException.class);
  }

  @Test
  @DisplayName("should throw RateLimitException when user limit is exceeded")
  @SuppressWarnings("unchecked")
  void shouldThrowWhenUserLimitExceeded() {
    when(redisTemplate.execute(
            any(RedisScript.class), argThat(keys -> keys.contains(IP_KEY)), anyString()))
        .thenReturn(1L);
    when(redisTemplate.execute(
            any(RedisScript.class), argThat(keys -> keys.contains(USER_KEY)), anyString()))
        .thenReturn((long) MAX_ATTEMPTS_PER_USER + 1);

    assertThatThrownBy(() -> service.checkResendLimit(IP_ADDRESS, OPERATION, USER_EXTENSION_ID))
        .isInstanceOf(RateLimitException.class);
  }

  @Test
  @DisplayName("should check IP limit before user limit")
  @SuppressWarnings("unchecked")
  void shouldCheckIpBeforeUser() {
    when(redisTemplate.execute(
            any(RedisScript.class), argThat(keys -> keys.contains(IP_KEY)), anyString()))
        .thenReturn((long) MAX_ATTEMPTS_PER_IP + 1);

    assertThatThrownBy(() -> service.checkResendLimit(IP_ADDRESS, OPERATION, USER_EXTENSION_ID))
        .isInstanceOf(RateLimitException.class);

    verify(redisTemplate, times(1)).execute(any(RedisScript.class), anyList(), anyString());
  }

  @Test
  @DisplayName("should fail open when Redis throws an exception")
  @SuppressWarnings("unchecked")
  void shouldFailOpenOnRedisException() {
    when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
        .thenThrow(new RuntimeException("Redis unavailable"));

    assertThatNoException()
        .isThrownBy(() -> service.checkResendLimit(IP_ADDRESS, OPERATION, USER_EXTENSION_ID));
  }
}
