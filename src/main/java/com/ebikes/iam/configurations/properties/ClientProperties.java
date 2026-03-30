package com.ebikes.iam.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "clients.client")
@Getter
@Setter
public class ClientProperties {

  private String baseUrl;
  private VerificationConfiguration verification;

  @Getter
  @Setter
  public static class VerificationConfiguration {
    private ChannelConfiguration accountActivation;
    private ChannelConfiguration email;
    private ChannelConfiguration passwordReset;
  }

  @Getter
  @Setter
  public static class ChannelConfiguration {
    private String path;
  }
}
