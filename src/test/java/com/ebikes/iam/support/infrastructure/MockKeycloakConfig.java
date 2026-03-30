package com.ebikes.iam.support.infrastructure;

import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class MockKeycloakConfig {

  @Bean
  @Primary
  public Keycloak keycloakAdminClient() {
    return Mockito.mock(Keycloak.class);
  }
}
