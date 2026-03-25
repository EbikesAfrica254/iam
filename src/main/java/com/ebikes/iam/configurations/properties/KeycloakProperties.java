package com.ebikes.iam.configurations.properties;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "keycloak")
@Component
@Data
public class KeycloakProperties {

    @NotBlank(message = "'base-organization-id' must be configured.")
    private String baseOrganizationId = "00000000-0000-0000-0000-000000000000";

    @NotBlank(message = "'base-organization-name' must be configured.")
    private String baseOrganizationName = "Ebikes Africa";

    @NotBlank(message = "'client-id' must be configured.")
    private String clientId;

    @NotBlank(message = "'client-secret' must be configured.")
    private String clientSecret;

    @NotEmpty(message = "'group-mappings' must be configured.")
    private Map<String, String> groupMapping;

    @NotBlank(message = "realm must be configured.")
    private String realm;

    @NotBlank(message = "url must be configured.")
    private String url;

    @NotBlank(message = "'web-client-id' must be configured.")
    private String webClientId;
}
