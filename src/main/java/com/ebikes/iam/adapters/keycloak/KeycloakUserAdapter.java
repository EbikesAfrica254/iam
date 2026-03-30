package com.ebikes.iam.adapters.keycloak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ExternalServiceException;
import com.ebikes.iam.mappers.KeycloakUserMapper;
import com.ebikes.iam.support.keycloak.KeycloakResponseUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserAdapter {

  private static final String ENDPOINT = "keycloak://users";
  private static final String UPDATE_PASSWORD_ACTION = "UPDATE_PASSWORD";
  private static final String VERIFY_EMAIL_ACTION = "VERIFY_EMAIL";

  private final Keycloak keycloakAdminClient;
  private final KeycloakProperties keycloakProperties;
  private final KeycloakUserMapper keycloakUserMapper;

  public String createUser(String email, String firstName, String lastName, String username) {
    try (Response response =
        getUsersResource()
            .create(
                keycloakUserMapper.toUserRepresentation(email, firstName, lastName, username))) {
      String keycloakUserId = KeycloakResponseUtilities.extractIdFromLocation(response);
      log.info("Keycloak user created - email={} keycloakUserId={}", email, keycloakUserId);
      return keycloakUserId;
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT, "Failed to create Keycloak user", ResponseCode.EXTERNAL_SERVICE_ERROR, e);
    }
  }

  public void deleteUser(String keycloakUserId) {
    try {
      getUsersResource().get(keycloakUserId).remove();
      log.info("Keycloak user deleted - keycloakUserId={}", keycloakUserId);
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT, "Failed to delete Keycloak user", ResponseCode.EXTERNAL_SERVICE_ERROR, e);
    }
  }

  public void disableUser(String keycloakUserId) {
    toggleUserEnabledStatus(false, keycloakUserId);
  }

  public void enableUser(String keycloakUserId) {
    toggleUserEnabledStatus(true, keycloakUserId);
  }

  public boolean isEmailRegistered(String email) {
    try {
      return !getUsersResource().searchByEmail(email, true).isEmpty();
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT,
          "Failed to search Keycloak users by email",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }

  public void resetPassword(String keycloakUserId, String password, boolean temporary) {
    try {
      UserResource userResource = getUserResource(keycloakUserId);

      CredentialRepresentation credential = new CredentialRepresentation();
      credential.setType(CredentialRepresentation.PASSWORD);
      credential.setValue(password);
      credential.setTemporary(temporary);

      userResource.resetPassword(credential);

      if (temporary) {
        ensureUpdatePasswordAction(userResource);
      }

      log.info("Password reset - keycloakUserId={} temporary={}", keycloakUserId, temporary);
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT,
          "Failed to reset Keycloak user password",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }

  public void updateUser(String keycloakUserId, UpdateUserExtensionRequest request) {
    try {
      UserResource userResource = getUserResource(keycloakUserId);
      UserRepresentation user = userResource.toRepresentation();
      keycloakUserMapper.updateUserRepresentation(request, user);
      userResource.update(user);
      log.info("User updated - keycloakUserId={}", keycloakUserId);
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT, "Failed to update Keycloak user", ResponseCode.EXTERNAL_SERVICE_ERROR, e);
    }
  }

  public void updateUserAttributes(String keycloakUserId, Map<String, String> attributes) {
    try {
      UserResource userResource = getUserResource(keycloakUserId);
      UserRepresentation user = userResource.toRepresentation();

      if (user.getAttributes() == null) {
        user.setAttributes(new HashMap<>());
      }

      attributes.forEach((key, value) -> user.getAttributes().put(key, List.of(value)));
      userResource.update(user);

      log.info(
          "Updated user attributes - keycloakUserId={} attributeKeys={}",
          keycloakUserId,
          attributes.keySet());
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT,
          "Failed to update Keycloak user attributes",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }

  public void verifyEmail(String keycloakUserId) {
    try {
      UserResource userResource = getUserResource(keycloakUserId);
      UserRepresentation user = userResource.toRepresentation();

      if (Boolean.TRUE.equals(user.isEmailVerified())) {
        log.debug("Email already verified - keycloakUserId={}", keycloakUserId);
        return;
      }

      user.setEmailVerified(true);
      removeRequiredAction(user);
      userResource.update(user);

      log.info("Email verified - keycloakUserId={}", keycloakUserId);
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT, "Failed to verify Keycloak user email", ResponseCode.EXTERNAL_SERVICE_ERROR, e);
    }
  }

  public void verifyPhone(String keycloakUserId) {
    try {
      UserResource userResource = getUserResource(keycloakUserId);
      UserRepresentation user = userResource.toRepresentation();

      String isVerified = user.getAttributes().get("phoneNumberVerified").getFirst();
      if (Objects.equals(isVerified, "true")) {
        log.debug("Phone number already verified - keycloakUserId={}", keycloakUserId);
        return;
      }

      user.getAttributes().put("phoneNumberVerified", List.of("true"));
      userResource.update(user);
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT,
          "Failed to verify Keycloak user phone number",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }

  private void ensureUpdatePasswordAction(UserResource userResource) {
    UserRepresentation user = userResource.toRepresentation();
    List<String> requiredActions =
        user.getRequiredActions() != null ? user.getRequiredActions() : new ArrayList<>();

    if (!requiredActions.contains(UPDATE_PASSWORD_ACTION)) {
      requiredActions.add(UPDATE_PASSWORD_ACTION);
      user.setRequiredActions(requiredActions);
      userResource.update(user);
    }
  }

  private UserResource getUserResource(String keycloakUserId) {
    return realmResource().users().get(keycloakUserId);
  }

  private UsersResource getUsersResource() {
    return realmResource().users();
  }

  private RealmResource realmResource() {
    return keycloakAdminClient.realm(keycloakProperties.getRealm());
  }

  private void removeRequiredAction(UserRepresentation user) {
    List<String> requiredActions = user.getRequiredActions();
    if (requiredActions != null && requiredActions.contains(VERIFY_EMAIL_ACTION)) {
      requiredActions.remove(VERIFY_EMAIL_ACTION);
      user.setRequiredActions(requiredActions);
    }
  }

  private void toggleUserEnabledStatus(boolean enabled, String keycloakUserId) {
    try {
      UserResource userResource = getUserResource(keycloakUserId);
      UserRepresentation user = userResource.toRepresentation();

      if (Boolean.TRUE.equals(user.isEnabled()) == enabled) {
        return;
      }

      user.setEnabled(enabled);
      userResource.update(user);
      log.info("User {} - keycloakUserId={}", enabled ? "enabled" : "disabled", keycloakUserId);
    } catch (ProcessingException e) {
      throw new ExternalServiceException(
          ENDPOINT,
          "Failed to " + (enabled ? "enable" : "disable") + " Keycloak user",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }
}
