package com.ebikes.iam.configurations.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "contact")
@Data
public class ContactProperties {

  private int expiryHours;
  private int staleThresholdHours;
}
