package com.ebikes.iam.support.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ebikes.iam.configurations.properties.SecurityProperties;
import com.ebikes.iam.configurations.security.SecurityConfiguration;
import com.ebikes.iam.support.fixtures.SecurityFixtures;

@ActiveProfiles("test")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
@Import({
  AbstractControllerTest.TestConfig.class,
  SecurityConfiguration.class,
  MockKeycloakConfig.class
})
@Tag("controller")
public abstract class AbstractControllerTest {

  @MockitoBean protected JwtDecoder jwtDecoder;

  @TestConfiguration
  static class TestConfig {

    @Bean
    public SecurityProperties securityProperties() {
      SecurityProperties props = new SecurityProperties();
      props.setPublicEndpoints(
          List.of(
              "/actuator/health",
              "/actuator/prometheus",
              "/password-reset/**",
              "/users/signup",
              "/verification/**"));
      props.setScopes(List.of("email", "openid", "profile"));
      return props;
    }
  }

  protected JwtRequestPostProcessor authenticatedJwt() {
    return SecurityFixtures.authenticatedJwt();
  }

  protected RequestPostProcessor anonymous() {
    return SecurityFixtures.anonymous();
  }
}
