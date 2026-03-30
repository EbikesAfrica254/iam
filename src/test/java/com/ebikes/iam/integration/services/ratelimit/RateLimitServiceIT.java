package com.ebikes.iam.integration.services.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.ebikes.iam.exceptions.RateLimitException;
import com.ebikes.iam.services.ratelimit.RateLimitService;
import com.ebikes.iam.support.infrastructure.AbstractIntegrationTest;

class RateLimitServiceIT extends AbstractIntegrationTest {

  private static final String IP_ADDRESS = "127.0.0.1";
  private static final String OPERATION = "EMAIL_VERIFICATION";
  private static final String USER_ID = "test-user-001";

  @Autowired private RateLimitService rateLimitService;
  @Autowired private RedisTemplate<String, String> redisTemplate;

  @BeforeEach
  void setUp() {
    assert redisTemplate.getConnectionFactory() != null;
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Nested
  class CheckResendLimit {

    @Test
    @DisplayName("passes when under limit")
    void passesWhenUnderLimit() {
      assertThat(
              catchThrowable(
                  () -> rateLimitService.checkResendLimit(IP_ADDRESS, OPERATION, USER_ID)))
          .isNull();
    }

    @Test
    @DisplayName("throws when IP limit is exceeded")
    void throwsWhenIpLimitExceeded() {
      exhaustIpLimit();

      assertThatThrownBy(() -> rateLimitService.checkResendLimit(IP_ADDRESS, OPERATION, USER_ID))
          .isInstanceOf(RateLimitException.class);
    }

    @Test
    @DisplayName("throws when user limit is exceeded")
    void throwsWhenUserLimitExceeded() {
      exhaustUserLimit();

      assertThatThrownBy(() -> rateLimitService.checkResendLimit(IP_ADDRESS, OPERATION, USER_ID))
          .isInstanceOf(RateLimitException.class);
    }

    @Test
    @DisplayName("TTL is set on first increment only")
    void ttlIsSetOnFirstIncrementOnly() {
      String key = "ratelimit:resend:ip:" + IP_ADDRESS + ":" + OPERATION;

      rateLimitService.checkResendLimit(IP_ADDRESS, OPERATION, USER_ID);
      Long ttlAfterFirst = redisTemplate.getExpire(key);

      rateLimitService.checkResendLimit(IP_ADDRESS, OPERATION, USER_ID);
      Long ttlAfterSecond = redisTemplate.getExpire(key);

      assertThat(ttlAfterFirst).isPositive();
      assertThat(ttlAfterSecond).isLessThanOrEqualTo(ttlAfterFirst);
    }

    private void exhaustIpLimit() {
      for (int i = 0; i <= 10; i++) {
        try {
          rateLimitService.checkResendLimit(IP_ADDRESS, OPERATION, "different-user-" + i);
        } catch (RateLimitException ignored) {
          break;
        }
      }
    }

    private void exhaustUserLimit() {
      for (int i = 0; i <= 5; i++) {
        try {
          rateLimitService.checkResendLimit("different-ip-" + i, OPERATION, USER_ID);
        } catch (RateLimitException ignored) {
          break;
        }
      }
    }
  }
}
