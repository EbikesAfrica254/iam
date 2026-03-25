package com.ebikes.iam.configurations;

import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ExternalServiceException;
import com.ebikes.iam.exceptions.RateLimitException;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KeycloakConfiguration {

  private static final String ENDPOINT_PREFIX = "keycloak://";

  private final KeycloakProperties keycloakProperties;
  private final ObjectMapper objectMapper;

  @Bean(destroyMethod = "close")
  public Keycloak keycloakAdminClient(ResteasyClient resteasyClient) {
    log.info("Initializing Keycloak admin client - server={}", keycloakProperties.getUrl());

    return KeycloakBuilder.builder()
        .clientId(keycloakProperties.getClientId())
        .clientSecret(keycloakProperties.getClientSecret())
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .realm(keycloakProperties.getRealm())
        .serverUrl(keycloakProperties.getUrl())
        .resteasyClient(resteasyClient)
        .build();
  }

  @Bean(destroyMethod = "close")
  public ResteasyClient resteasyClient() {
    return new ResteasyClientBuilderImpl()
        .connectTimeout(ApplicationConstants.HttpClient.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApplicationConstants.HttpClient.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .register(keycloakErrorResponseFilter())
        .build();
  }

  private ClientResponseFilter keycloakErrorResponseFilter() {
    return (requestContext, responseContext) -> {
      int status = responseContext.getStatus();

      if (status < 400) {
        return;
      }

      String endpoint = ENDPOINT_PREFIX + requestContext.getUri().getPath();
      KeycloakError error = parseKeycloakError(responseContext);

      log.error(
          "Keycloak error response - endpoint={} status={} error={} description={}",
          endpoint,
          status,
          error.error(),
          error.errorDescription());

      if (status == 429) {
        throw new RateLimitException(
            ResponseCode.RATE_LIMIT_EXCEEDED,
            descriptionOrDefault(error.errorDescription(), "Keycloak rate limit exceeded"));
      }

      if (status == 401 || status == 403 || isAuthenticationError(error.error())) {
        throw new ExternalServiceException(
            endpoint,
            "Keycloak authentication failed: " + error.errorDescription(),
            ResponseCode.AUTHENTICATION_FAILED);
      }

      if (status == 404) {
        throw new ExternalServiceException(
            endpoint,
            "Keycloak resource not found: " + error.errorDescription(),
            ResponseCode.RESOURCE_NOT_FOUND);
      }

      if (status == 409) {
        throw new ExternalServiceException(
            endpoint,
            "Keycloak conflict: " + error.errorDescription(),
            ResponseCode.DUPLICATE_RESOURCE);
      }

      throw new ExternalServiceException(
          endpoint,
          descriptionOrDefault(error.errorDescription(), "Keycloak API error"),
          ResponseCode.fromHttpStatus(status));
    };
  }

  private String descriptionOrDefault(String description, String fallback) {
    return description != null ? description : fallback;
  }

  private boolean isAuthenticationError(String error) {
    return "invalid_client".equals(error)
        || "unauthorized_client".equals(error)
        || "invalid_token".equals(error);
  }

  private KeycloakError parseKeycloakError(ClientResponseContext responseContext) {
    try {
      InputStream body = responseContext.getEntityStream();
      if (body == null || body.available() == 0) {
        return new KeycloakError(null, null);
      }
      byte[] bytes = body.readAllBytes();
      responseContext.setEntityStream(new java.io.ByteArrayInputStream(bytes));
      JsonNode root = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
      return new KeycloakError(
          root.path("error").asString(null), root.path("error_description").asString(null));
    } catch (Exception e) {
      log.debug("Failed to parse Keycloak error response", e);
      return new KeycloakError(null, null);
    }
  }

  private record KeycloakError(String error, String errorDescription) {}
}
