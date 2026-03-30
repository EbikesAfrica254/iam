package com.ebikes.iam.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "services.organizations")
@Getter
@Setter
public class OrganizationServiceProperties {

  private String baseUrl;
}
