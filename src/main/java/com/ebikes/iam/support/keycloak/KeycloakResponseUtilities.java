package com.ebikes.iam.support.keycloak;

import java.net.URI;

import jakarta.ws.rs.core.Response;

public final class KeycloakResponseUtilities {

  private KeycloakResponseUtilities() {
    // prevent instantiation
  }

  public static String extractIdFromLocation(Response response) {
    URI location = response.getLocation();
    if (location == null) {
      throw new IllegalStateException("Keycloak response missing Location header");
    }
    String path = location.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }
}
