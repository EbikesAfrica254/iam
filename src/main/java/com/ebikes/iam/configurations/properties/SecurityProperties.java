package com.ebikes.iam.configurations.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "security")
@Validated
@Data
public class SecurityProperties {
  private List<String> publicEndpoints;
  private List<String> scopes;
}
