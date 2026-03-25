package com.ebikes.iam.configurations.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

  private ResendLimits resend = new ResendLimits();

  @Getter
  @Setter
  public static class ResendLimits {
    private int maxAttemptsPerUser = 5;
    private int maxAttemptsPerIp = 10;
    private int windowMinutes = 60;
  }
}
