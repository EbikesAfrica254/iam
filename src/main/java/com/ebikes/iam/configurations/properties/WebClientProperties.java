package com.ebikes.iam.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@ConfigurationProperties(prefix = "web-client")
@Component
@Data
@Validated
public class WebClientProperties {
  private String baseUrl;
  private String support;
  private VerificationConfiguration verification;

  @Data
  public static class VerificationConfiguration {
    private ChannelConfiguration email;
    private ChannelConfiguration passwordReset;
    private ChannelConfiguration phone;
  }

  @Data
  public static class ChannelConfiguration {
    private String path;
  }
}
