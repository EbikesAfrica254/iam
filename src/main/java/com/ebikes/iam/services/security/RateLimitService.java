package com.ebikes.iam.services.security;

import com.ebikes.iam.configurations.properties.RateLimitProperties;
import com.ebikes.iam.configurations.properties.RateLimitProperties.ResendLimits;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class RateLimitService {

    private static final String IP_KEY_PREFIX = "ratelimit:resend:ip:";
    private static final String USER_KEY_PREFIX = "ratelimit:resend:user:";

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT =
            RedisScript.of(
                    """
                            local current = redis.call('incr', KEYS[1])
                            redis.call('expire', KEYS[1], ARGV[1])
                            return current
                            """,
                    Long.class);

    private final RateLimitProperties rateLimitProperties;
    private final RedisTemplate<String, String> redisTemplate;

    public void checkResendLimit(String ipAddress, String operation, String userExtensionId) {
        ResendLimits config = rateLimitProperties.getResend();
        Duration window = Duration.ofMinutes(config.getWindowMinutes());

        checkIpLimit(ipAddress, operation, window, config.getMaxAttemptsPerIp());
        checkUserLimit(operation, userExtensionId, window, config.getMaxAttemptsPerUser());
    }

    private void checkIpLimit(String ipAddress, String operation, Duration window, int maxAttempts) {
        String key = IP_KEY_PREFIX + ipAddress + ":" + operation;
        Long attempts = executeRateLimitCheck(key, window);

        if (attempts > maxAttempts) {
            log.warn(
                    "Rate limit exceeded for IP - operation={} attempts={} limit={}",
                    operation,
                    attempts,
                    maxAttempts);
            throw new RateLimitException(
                    ResponseCode.RATE_LIMIT_EXCEEDED,
                    "Too many resend attempts from this location. Please try again later.");
        }

        log.debug(
                "Rate limit check passed for IP - operation={} attempts={} limit={}",
                operation,
                attempts,
                maxAttempts);
    }

    private void checkUserLimit(
            String operation, String userExtensionId, Duration window, int maxAttempts) {
        String key = USER_KEY_PREFIX + userExtensionId + ":" + operation;
        Long attempts = executeRateLimitCheck(key, window);

        if (attempts > maxAttempts) {
            log.warn(
                    "Rate limit exceeded for user - userExtensionId={} operation={} attempts={} limit={}",
                    userExtensionId,
                    operation,
                    attempts,
                    maxAttempts);
            throw new RateLimitException(
                    ResponseCode.RATE_LIMIT_EXCEEDED, "Too many resend attempts. Please try again later.");
        }

        log.debug(
                "Rate limit check passed for user - userExtensionId={} operation={} attempts={} limit={}",
                userExtensionId,
                operation,
                attempts,
                maxAttempts);
    }

    private Long executeRateLimitCheck(String key, Duration window) {
        try {
            return redisTemplate.execute(
                    RATE_LIMIT_SCRIPT, List.of(key), String.valueOf(window.getSeconds()));
        } catch (Exception e) {
            log.error("Redis rate limit check failed for key={}, failing open", key, e);
            return 0L;
        }
    }
}
