package com.ebikes.iam.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "token")
@Data
public class TokenProperties {

  private TokenConfiguration accountActivation;
  private TokenConfiguration emailOtp;
  private TokenConfiguration emailVerification;
  private TokenConfiguration passwordReset;
  private TokenConfiguration phoneNumberVerification;
  private TokenConfiguration smsOtp;
  private TokenConfiguration twoFactorAuth;

  @Data
  public static class TokenConfiguration {
    private int length;
    private Integer validityMinutes;
    private Integer validityHours;
  }
}
