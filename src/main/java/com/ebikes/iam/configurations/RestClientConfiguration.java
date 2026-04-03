package com.ebikes.iam.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.web.client.RestClient;

import com.ebikes.iam.configurations.properties.OrganizationServiceProperties;

@Configuration
public class RestClientConfiguration {

  @Bean
  public RestClient organizationServiceRestClient(
      RestClient.Builder restClientBuilder,
      OAuth2AuthorizedClientManager authorizedClientManager,
      OAuth2AuthorizedClientService authorizedClientService,
      OrganizationServiceProperties organizationServiceProperties) {

    OAuth2ClientHttpRequestInterceptor requestInterceptor =
        new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);

    requestInterceptor.setAuthorizationFailureHandler(
        OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(authorizedClientService));

    requestInterceptor.setClientRegistrationIdResolver(
        request -> organizationServiceProperties.getClientRegistrationId());

    requestInterceptor.setPrincipalResolver(new RequestAttributePrincipalResolver());

    return restClientBuilder
        .baseUrl(organizationServiceProperties.getBaseUrl())
        .requestInterceptor(requestInterceptor)
        .build();
  }
}
