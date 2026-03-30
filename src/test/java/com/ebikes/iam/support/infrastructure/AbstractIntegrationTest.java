package com.ebikes.iam.support.infrastructure;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.support.fixtures.SecurityFixtures;

@ActiveProfiles({"test", "integration-test"})
@AutoConfigureMockMvc
@Import({IntegrationContainersConfig.class, MockKeycloakConfig.class})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Tag("integration")
public abstract class AbstractIntegrationTest {

  @Autowired protected MockMvc mockMvc;

  @MockitoBean protected JwtDecoder jwtDecoder;

  protected JwtRequestPostProcessor authenticatedJwt() {
    return SecurityFixtures.authenticatedJwt();
  }

  protected JwtRequestPostProcessor authenticatedJwt(UserRole... roles) {
    return SecurityFixtures.authenticatedJwt(roles);
  }

  protected RequestPostProcessor anonymous() {
    return SecurityFixtures.anonymous();
  }
}
